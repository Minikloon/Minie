/*
 Copyright (c) 2020-2023, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3utilities.tutorial;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.RotationOrder;
import com.jme3.bullet.collision.shapes.Box2dShape;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.debug.DebugInitListener;
import com.jme3.bullet.joints.New6Dof;
import com.jme3.bullet.joints.motors.MotorParam;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import jme3utilities.MeshNormals;

/**
 * A simple example of a PhysicsJoint with limits.
 * <p>
 * Builds upon HelloJoint.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloLimit
        extends SimpleApplication
        implements PhysicsTickListener {
    // *************************************************************************
    // constants

    /**
     * physics-space Y coordinate of the ground plane
     */
    private final static float groundY = -2f;
    /**
     * half the height of the paddle (in physics-space units)
     */
    private final static float paddleHalfHeight = 1f;
    // *************************************************************************
    // fields

    /**
     * mouse-controlled kinematic paddle
     */
    private PhysicsRigidBody paddleBody;
    /**
     * PhysicsSpace for simulation
     */
    private PhysicsSpace physicsSpace;
    /**
     * latest ground location indicated by the mouse cursor
     */
    private final Vector3f mouseLocation = new Vector3f();
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloLimit application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        HelloLimit application = new HelloLimit();

        boolean loadDefaults = true;
        AppSettings settings = new AppSettings(loadDefaults);

        // Enable gamma correction for accurate lighting.
        settings.setGammaCorrection(true);

        // Disable VSync for more frequent mouse-position updates.
        settings.setVSync(false);
        application.setSettings(settings);

        application.start();
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void simpleInitApp() {
        configureCamera();
        this.physicsSpace = configurePhysics();

        // Add a static, green square to represent the ground.
        float halfExtent = 3f;
        addSquare(halfExtent, groundY);

        // Add a mouse-controlled kinematic paddle.
        addPaddle();

        // Add a dynamic yellow ball.
        PhysicsRigidBody ballBody = addBall();

        // Add a single-ended physics joint to constrain the ball's center.
        Vector3f pivotInBall = new Vector3f(0f, 0f, 0f);
        Vector3f pivotInWorld = new Vector3f(0f, 0f, 0f);
        Matrix3f rotInBall = Matrix3f.IDENTITY;
        Matrix3f rotInPaddle = Matrix3f.IDENTITY;
        New6Dof joint = new New6Dof(ballBody, pivotInBall, pivotInWorld,
                rotInBall, rotInPaddle, RotationOrder.XYZ);
        physicsSpace.addJoint(joint);

        // Limit the X and Z translation DOFs.
        joint.set(MotorParam.LowerLimit, PhysicsSpace.AXIS_X, -halfExtent);
        joint.set(MotorParam.LowerLimit, PhysicsSpace.AXIS_Z, -halfExtent);
        joint.set(MotorParam.UpperLimit, PhysicsSpace.AXIS_X, +halfExtent);
        joint.set(MotorParam.UpperLimit, PhysicsSpace.AXIS_Z, +halfExtent);

        // Lock the Y translation at paddle height.
        float paddleY = groundY + paddleHalfHeight;
        joint.set(MotorParam.LowerLimit, PhysicsSpace.AXIS_Y, paddleY);
        joint.set(MotorParam.UpperLimit, PhysicsSpace.AXIS_Y, paddleY);
    }

    /**
     * Callback invoked once per frame.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        // Calculate the ground location (if any) selected by the mouse cursor.
        Vector2f screenXY = inputManager.getCursorPosition();
        float nearZ = 0f;
        Vector3f nearLocation = cam.getWorldCoordinates(screenXY, nearZ);
        float farZ = 1f;
        Vector3f farLocation = cam.getWorldCoordinates(screenXY, farZ);
        if (nearLocation.y > groundY && farLocation.y < groundY) {
            float dy = nearLocation.y - farLocation.y;
            float t = (nearLocation.y - groundY) / dy;
            FastMath.interpolateLinear(
                    t, nearLocation, farLocation, mouseLocation);
        }
    }
    // *************************************************************************
    // PhysicsTickListener methods

    /**
     * Callback from Bullet, invoked just before each simulation step.
     *
     * @param space the space that's about to be stepped (not null)
     * @param timeStep the time per simulation step (in seconds, &ge;0)
     */
    @Override
    public void prePhysicsTick(PhysicsSpace space, float timeStep) {
        // Reposition the paddle based on the mouse location.
        Vector3f bodyLocation = mouseLocation.add(0f, paddleHalfHeight, 0f);
        paddleBody.setPhysicsLocation(bodyLocation);
    }

    /**
     * Callback from Bullet, invoked just after each simulation step.
     *
     * @param space the space that was just stepped (not null)
     * @param timeStep the time per simulation step (in seconds, &ge;0)
     */
    @Override
    public void physicsTick(PhysicsSpace space, float timeStep) {
        // do nothing
    }
    // *************************************************************************
    // private methods

    /**
     * Create a dynamic rigid body with a sphere shape and add it to the space.
     *
     * @return the new body
     */
    private PhysicsRigidBody addBall() {
        float radius = 0.4f;
        SphereCollisionShape shape = new SphereCollisionShape(radius);

        float mass = 0.2f;
        PhysicsRigidBody result = new PhysicsRigidBody(shape, mass);
        physicsSpace.addCollisionObject(result);

        // Apply angular damping to reduce the ball's tendency to spin.
        result.setAngularDamping(0.6f);

        // Disable sleep (deactivation).
        result.setEnableSleep(false);

        Material yellowMaterial = createLitMaterial(1f, 1f, 0f);
        result.setDebugMaterial(yellowMaterial);
        result.setDebugMeshNormals(MeshNormals.Facet);
        // faceted so that rotations will be visible

        return result;
    }

    /**
     * Add lighting and shadows to the specified scene and set the background
     * color.
     *
     * @param scene the scene to augment (not null)
     */
    private void addLighting(Spatial scene) {
        ColorRGBA ambientColor = new ColorRGBA(0.03f, 0.03f, 0.03f, 1f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        scene.addLight(ambient);
        ambient.setName("ambient");

        ColorRGBA directColor = new ColorRGBA(0.2f, 0.2f, 0.2f, 1f);
        Vector3f direction = new Vector3f(-7f, -3f, -5f).normalizeLocal();
        DirectionalLight sun = new DirectionalLight(direction, directColor);
        scene.addLight(sun);
        sun.setName("sun");

        // Render shadows based on the directional light.
        viewPort.clearProcessors();
        int shadowMapSize = 2_048; // in pixels
        int numSplits = 3;
        DirectionalLightShadowRenderer dlsr
                = new DirectionalLightShadowRenderer(
                        assetManager, shadowMapSize, numSplits);
        dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        dlsr.setEdgesThickness(5);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.6f);
        viewPort.addProcessor(dlsr);

        // Set the viewport's background color to light blue.
        ColorRGBA skyColor = new ColorRGBA(0.1f, 0.2f, 0.4f, 1f);
        viewPort.setBackgroundColor(skyColor);
    }

    /**
     * Create a kinematic body with a box shape and add it to the space.
     */
    private void addPaddle() {
        BoxCollisionShape shape
                = new BoxCollisionShape(0.3f, paddleHalfHeight, 1f);
        paddleBody = new PhysicsRigidBody(shape);
        paddleBody.setKinematic(true);
        physicsSpace.addCollisionObject(paddleBody);

        Material redMaterial = createLitMaterial(1f, 0.1f, 0.1f);
        paddleBody.setDebugMaterial(redMaterial);
        paddleBody.setDebugMeshNormals(MeshNormals.Facet);
    }

    /**
     * Add a horizontal square body to the space.
     *
     * @param halfExtent (half of the desired side length)
     * @param y (the desired elevation, in physics-space coordinates)
     * @return the new body (not null)
     */
    private PhysicsRigidBody addSquare(float halfExtent, float y) {
        // Construct a static rigid body with a square shape.
        Box2dShape shape = new Box2dShape(halfExtent);
        PhysicsRigidBody result
                = new PhysicsRigidBody(shape, PhysicsBody.massForStatic);

        Material greenMaterial = createLitMaterial(0f, 1f, 0f);
        result.setDebugMaterial(greenMaterial);
        result.setDebugMeshNormals(MeshNormals.Facet);

        physicsSpace.addCollisionObject(result);

        // Rotate it 90 degrees to a horizontal orientation.
        Quaternion rotate90 = new Quaternion()
                .fromAngles(-FastMath.HALF_PI, 0f, 0f);
        result.setPhysicsRotation(rotate90);

        // Translate it to the desired elevation.
        result.setPhysicsLocation(new Vector3f(0f, y, 0f));

        return result;
    }

    /**
     * Disable FlyByCamera during startup.
     */
    private void configureCamera() {
        flyCam.setEnabled(false);

        cam.setLocation(new Vector3f(0f, 5f, 10f));
        cam.setRotation(new Quaternion(0f, 0.95f, -0.3122f, 0f));
    }

    /**
     * Configure physics during startup.
     *
     * @return a new instance (not null)
     */
    private PhysicsSpace configurePhysics() {
        BulletAppState bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        // Enable debug visualization to reveal what occurs in physics space.
        bulletAppState.setDebugEnabled(true);

        // Add lighting and shadows to the debug scene.
        bulletAppState.setDebugInitListener(new DebugInitListener() {
            @Override
            public void bulletDebugInit(Node physicsDebugRootNode) {
                addLighting(physicsDebugRootNode);
            }
        });
        bulletAppState.setDebugShadowMode(
                RenderQueue.ShadowMode.CastAndReceive);

        PhysicsSpace result = bulletAppState.getPhysicsSpace();

        // To enable the callbacks, register the application as a tick listener.
        result.addTickListener(this);

        // Reduce the time step for better accuracy.
        result.setAccuracy(0.005f);

        return result;
    }

    /**
     * Create a single-sided lit material with the specified reflectivities.
     *
     * @param red the desired reflectivity for red light (&ge;0, &le;1)
     * @param green the desired reflectivity for green light (&ge;0, &le;1)
     * @param blue the desired reflectivity for blue light (&ge;0, &le;1)
     * @return a new instance (not null)
     */
    private Material createLitMaterial(float red, float green, float blue) {
        Material result = new Material(assetManager, Materials.LIGHTING);
        result.setBoolean("UseMaterialColors", true);

        float opacity = 1f;
        result.setColor("Ambient", new ColorRGBA(red, green, blue, opacity));
        result.setColor("Diffuse", new ColorRGBA(red, green, blue, opacity));

        return result;
    }
}
