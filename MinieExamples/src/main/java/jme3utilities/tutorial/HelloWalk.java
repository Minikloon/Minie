/*
 Copyright (c) 2020, Stephen Gold
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
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.collision.shapes.infos.DebugMeshNormals;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.debug.DebugInitListener;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsCharacter;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.input.CameraInput;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.system.AppSettings;
import com.jme3.terrain.heightmap.HeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;

/**
 * A simple example of character physics.
 *
 * Press the W key to walk. Press the space bar to jump.
 *
 * Builds upon HelloCharacter.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloWalk
        extends SimpleApplication
        implements ActionListener, PhysicsTickListener {
    // *************************************************************************
    // fields

    /**
     * true when the spacebar is pressed, otherwise false
     */
    private volatile boolean jumpRequested;
    /**
     * true when the W key is pressed, otherwise false
     */
    private volatile boolean walkRequested;
    private PhysicsCharacter character;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloWalk application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        HelloWalk application = new HelloWalk();

        // Enable gamma correction for accurate lighting.
        boolean loadDefaults = true;
        AppSettings settings = new AppSettings(loadDefaults);
        settings.setGammaCorrection(true);
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
        configureInput();
        PhysicsSpace physicsSpace = configurePhysics();

        // Create a character with a capsule shape and add it to the space.
        float characterRadius = 0.5f;
        float characterHeight = 1f;
        CapsuleCollisionShape characterShape
                = new CapsuleCollisionShape(characterRadius, characterHeight);
        float stepHeight = 0.3f;
        character = new PhysicsCharacter(characterShape, stepHeight);
        physicsSpace.addCollisionObject(character);

        // Teleport the character to its initial location.
        character.setPhysicsLocation(new Vector3f(-74f, 16f, -47f));

        // Add a static heightmap to represent the ground.
        addTerrain(physicsSpace);
    }

    /**
     * Callback invoked once per frame.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        // Synchronize the (first-person) camera location
        // with the PhysicsCharacter.
        // This overrides any translation requested by FlyByCamera.
        Vector3f location = character.getPhysicsLocation(null);
        cam.setLocation(location);
    }
    // *************************************************************************
    // ActionListener methods

    /**
     * Callback to handle keyboard input events.
     *
     * @param action the name of the input event
     * @param ongoing true &rarr; pressed, false &rarr; released
     * @param tpf the time per frame (in seconds, &ge;0)
     */
    @Override
    public void onAction(String action, boolean ongoing, float tpf) {
        switch (action) {
            case CameraInput.FLYCAM_FORWARD:
                walkRequested = ongoing;
                return;

            case "jump":
                jumpRequested = ongoing;
                return;

            default:
                System.out.println("Unknown action: " + action);
        }
    }
    // *************************************************************************
    // PhysicsTickListener methods

    /**
     * Callback from Bullet, invoked just before the physics is stepped.
     *
     * @param space the space that is about to be stepped (not null)
     * @param timeStep the time per physics step (in seconds, &ge;0)
     */
    @Override
    public void prePhysicsTick(PhysicsSpace space, float timeStep) {
        // Clear any motion from the previous tick.
        character.setWalkDirection(Vector3f.ZERO);

        // If the character is touching the ground,
        // cause it respond to keyboard input.
        if (character.onGround()) {
            if (jumpRequested) {
                character.jump();

            } else if (walkRequested) {
                // Walk in the camera's forward direction.
                Vector3f offset = cam.getDirection();
                float walkSpeed = 5f;
                offset.multLocal(walkSpeed * timeStep);
                character.setWalkDirection(offset);
            }
        }
    }

    /**
     * Callback from Bullet, invoked just after the physics has been stepped.
     *
     * @param space the space that was just stepped (not null)
     * @param timeStep the time per physics step (in seconds, &ge;0)
     */
    @Override
    public void physicsTick(PhysicsSpace space, float timeStep) {
        // do nothing
    }
    // *************************************************************************
    // private methods

    /**
     * Add lighting and shadows to the specified scene.
     */
    private void addLighting(Spatial scene) {
        ColorRGBA ambientColor = new ColorRGBA(0.03f, 0.03f, 0.03f, 1f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        scene.addLight(ambient);
        ambient.setName("ambient");

        ColorRGBA directColor = new ColorRGBA(0.1f, 0.1f, 0.1f, 1f);
        Vector3f direction = new Vector3f(-7f, -3f, -5f).normalizeLocal();
        DirectionalLight sun = new DirectionalLight(direction, directColor);
        scene.addLight(sun);
        sun.setName("sun");

        // Render shadows based on the directional light.
        viewPort.clearProcessors();
        int shadowMapSize = 2_048; // in pixels
        int numSplits = 3;
        DirectionalLightShadowRenderer dlsr
                = new DirectionalLightShadowRenderer(assetManager,
                        shadowMapSize, numSplits);
        dlsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        dlsr.setEdgesThickness(5);
        dlsr.setLight(sun);
        dlsr.setShadowIntensity(0.4f);
        viewPort.addProcessor(dlsr);

        // Set the viewport's background color to light blue.
        ColorRGBA skyColor = new ColorRGBA(0.1f, 0.2f, 0.4f, 1f);
        viewPort.setBackgroundColor(skyColor);
    }

    /**
     * Add a heighfield body to the specified PhysicsSpace.
     *
     * @param physicsSpace (not null)
     */
    private void addTerrain(PhysicsSpace physicsSpace) {
        // Generate a HeightMap from from jme3-testdata-3.1.0-stable.jar
        String assetPath = "Textures/Terrain/splat/mountains512.png";
        Texture texture = assetManager.loadTexture(assetPath);
        Image image = texture.getImage();
        HeightMap heightMap = new ImageBasedHeightMap(image);
        heightMap.setHeightScale(0.2f);

        // Construct a static rigid body based on the HeightMap.
        CollisionShape shape = new HeightfieldCollisionShape(heightMap);
        PhysicsRigidBody body
                = new RigidBodyControl(shape, PhysicsBody.massForStatic);

        physicsSpace.addCollisionObject(body);

        // Customize its debug visualization.
        Material greenMaterial = createLitMaterial(0f, 1f, 0f);
        body.setDebugMaterial(greenMaterial);
        body.setDebugMeshNormals(DebugMeshNormals.Smooth);
    }

    /**
     * Configure the Camera during startup.
     */
    private void configureCamera() {
        float frustumWidth = cam.getFrustumTop() - cam.getFrustumBottom();
        float fructumHeight = cam.getFrustumRight() - cam.getFrustumLeft();
        float aspectRatio = fructumHeight / frustumWidth;
        float far = cam.getFrustumFar();
        float fieldOfViewDegrees = 30f;

        // Bring the near plane closer to reduce clipping.
        float near = 0.1f; // default = 1
        cam.setFrustumPerspective(fieldOfViewDegrees, aspectRatio, near, far);
    }

    /**
     * Configure keyboard input during startup.
     */
    private void configureInput() {
        inputManager.addMapping("jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "jump", CameraInput.FLYCAM_FORWARD);
    }

    /**
     * Configure physics during startup.
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

        // Activate the PhysicsTickListener interface.
        result.addTickListener(this);

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
