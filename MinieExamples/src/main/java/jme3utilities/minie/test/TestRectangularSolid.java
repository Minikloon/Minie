/*
 Copyright (c) 2018-2021, Stephen Gold
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
package jme3utilities.minie.test;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.collision.shapes.MultiSphere;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.util.DebugShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.input.CameraInput;
import com.jme3.input.KeyInput;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.system.AppSettings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyAsset;
import jme3utilities.MyCamera;
import jme3utilities.MyString;
import jme3utilities.math.RectangularSolid;
import jme3utilities.mesh.PointMesh;
import jme3utilities.minie.test.common.PhysicsDemo;
import jme3utilities.minie.test.shape.ShapeGenerator;
import jme3utilities.ui.CameraOrbitAppState;
import jme3utilities.ui.InputMode;

/**
 * Test collision-shape fitting to pseudo-random ellipsoids using the
 * RectangularSolid class.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestRectangularSolid extends PhysicsDemo {
    // *************************************************************************
    // constants and loggers

    /**
     * color for visualizing sample points (white)
     */
    final private static ColorRGBA sampleColor = new ColorRGBA(1f, 1f, 1f, 1f);
    /**
     * size for visualizing sample points (in pixels)
     */
    final private static float samplePointSize = 2f;
    /**
     * number of sample points per trial
     */
    final private static int samplesPerTrial = 100;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(TestRectangularSolid.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = TestRectangularSolid.class.getSimpleName();
    // *************************************************************************
    // fields

    /**
     * status displayed in the upper-left corner of the GUI node
     */
    private BitmapText statusText;
    /**
     * AppState to manage the PhysicsSpace
     */
    private BulletAppState bulletAppState;
    /**
     * pseudo-random seed for the current/next trial
     */
    private long trialSeed = 1L;
    /**
     * scene-graph node for the current trial
     */
    private Node trialNode = null;
    /**
     * latest rigid body
     */
    private PhysicsRigidBody rigidBody;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the TestRectangularSolid application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        /*
         * Mute the chatty loggers in certain packages.
         */
        Heart.setLoggingLevels(Level.WARNING);

        Application application = new TestRectangularSolid();
        /*
         * Customize the window's title bar.
         */
        boolean loadDefaults = true;
        AppSettings settings = new AppSettings(loadDefaults);
        settings.setTitle(applicationName);

        settings.setAudioRenderer(null);
        settings.setGammaCorrection(true);
        settings.setSamples(4); // anti-aliasing
        settings.setVSync(true);
        application.setSettings(settings);

        application.start();
    }
    // *************************************************************************
    // PhysicsDemo methods

    /**
     * Initialize this application.
     */
    @Override
    public void actionInitializeApplication() {
        configureCamera();
        /*
         * Add the status text to the GUI.
         */
        statusText = new BitmapText(guiFont);
        statusText.setLocalTranslation(0f, cam.getHeight(), 0f);
        guiNode.attachChild(statusText);

        Material material = MyAsset.createWireframeMaterial(assetManager,
                sampleColor, samplePointSize);
        registerMaterial("samplePoint", material);

        bulletAppState = new BulletAppState();
        bulletAppState.setDebugEnabled(true);
        bulletAppState.setSpeed(0f);
        stateManager.attach(bulletAppState);

        ColorRGBA darkGray = new ColorRGBA(0.03f, 0.03f, 0.03f, 1f);
        viewPort.setBackgroundColor(darkGray);

        trial("capsule");
    }

    /**
     * Access the active BulletAppState.
     *
     * @return the pre-existing instance (not null)
     */
    @Override
    protected BulletAppState getBulletAppState() {
        assert bulletAppState != null;
        return bulletAppState;
    }

    /**
     * Determine the length of physics-debug arrows when visible.
     *
     * @return the desired length (in physics-space units, &ge;0)
     */
    @Override
    protected float maxArrowLength() {
        return 1f;
    }

    /**
     * Add application-specific hotkey bindings and override existing ones.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();

        dim.bind("next trial capsule", KeyInput.KEY_F3);
        dim.bind("next trial rounded", KeyInput.KEY_F1);
        dim.bind("next trial square", KeyInput.KEY_F2);

        dim.bindSignal(CameraInput.FLYCAM_LOWER, KeyInput.KEY_DOWN);
        dim.bindSignal(CameraInput.FLYCAM_RISE, KeyInput.KEY_UP);
        dim.bindSignal("orbitLeft", KeyInput.KEY_LEFT);
        dim.bindSignal("orbitRight", KeyInput.KEY_RIGHT);

        dim.bind(asToggleHelp, KeyInput.KEY_H);

        float margin = 10f; // in pixels
        float width = cam.getWidth() - 2f * margin;
        float height = cam.getHeight() - (2f * margin + 20f);
        float leftX = margin;
        float topY = margin + height;
        Rectangle rectangle = new Rectangle(leftX, topY, width, height);

        attachHelpNode(rectangle);
    }

    /**
     * Process an action that wasn't handled by the active input mode.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        if (ongoing) {
            if (actionString.startsWith("next trial ")) {
                String shapeName
                        = MyString.remainder(actionString, "next trial ");
                nextTrial(shapeName);
                return;
            }
        }
        super.onAction(actionString, ongoing, tpf);
    }
    // *************************************************************************
    // private methods

    /**
     * Configure the camera during startup.
     */
    private void configureCamera() {
        float near = 10f;
        float far = 10000f;
        MyCamera.setNearFar(cam, near, far);

        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(400f);
        flyCam.setZoomSpeed(400f);

        cam.setLocation(new Vector3f(0f, 0f, 1000f));

        AppState orbitState
                = new CameraOrbitAppState(cam, "orbitLeft", "orbitRight");
        stateManager.attach(orbitState);
    }

    /**
     * Perform a new trial after cleaning up from the previous one.
     *
     * @param shapeName type of collision shape to generate:
     * "square"&rarr;HullCollisionShape, "capsule"&rarr;MultiSphere with 2
     * spheres, or "rounded"&rarr;MultiSphere with 4 spheres
     */
    private void nextTrial(String shapeName) {
        if (trialNode != null) {
            PhysicsSpace space = bulletAppState.getPhysicsSpace();
            space.removeCollisionObject(rigidBody);
            rigidBody = null;

            trialNode.removeFromParent();
            trialNode = null;
        }
        trial(shapeName);
    }

    /**
     * Perform a new trial.
     *
     * @param shapeName type of collision shape to generate:
     * "square"&rarr;HullCollisionShape, "capsule"&rarr;MultiSphere with 2
     * spheres, "rounded"&rarr;MultiSphere with 4 spheres
     */
    private void trial(String shapeName) {
        trialNode = new Node("trialNode");
        rootNode.attachChild(trialNode);

        String message = "trialSeed = " + trialSeed;
        System.out.println(message);
        statusText.setText(message);
        ShapeGenerator random = getGenerator();
        random.setSeed(trialSeed);
        /*
         * Generate a new transform.
         */
        Transform transform = new Transform();
        Quaternion rotation = random.nextQuaternion();
        transform.setRotation(rotation);
        Vector3f scale = random.nextVector3f();
        scale.addLocal(1.2f, 1.2f, 1.2f);
        scale.multLocal(200f);
        transform.setScale(scale);
        Vector3f translation = random.nextVector3f();
        transform.setTranslation(translation);
        /*
         * Generate sample points on the surface of a transformed unit sphere
         * (which is an ellipsoid).
         */
        Collection<Vector3f> sampleLocations = new ArrayList<>(samplesPerTrial);
        for (int sampleIndex = 0;
                sampleIndex < samplesPerTrial;
                ++sampleIndex) {
            Vector3f sampleLocation = random.nextUnitVector3f();
            transform.transformVector(sampleLocation, sampleLocation);
            sampleLocations.add(sampleLocation);
        }
        /*
         * Visualize the sample points.
         */
        for (Vector3f location : sampleLocations) {
            PointMesh pointMesh = new PointMesh();
            pointMesh.setLocation(location);
            Geometry sampleGeometry = new Geometry("sample", pointMesh);
            Material material = findMaterial("samplePoint");
            sampleGeometry.setMaterial(material);
            trialNode.attachChild(sampleGeometry);
        }
        /*
         * Generate a rectangular solid that contains all the samples.
         */
        RectangularSolid solid = new RectangularSolid(sampleLocations);
        logger.log(Level.INFO, solid.toString());
        /*
         * Generate a collision shape.
         */
        CollisionShape collisionShape;
        switch (shapeName) {
            case "capsule":
                collisionShape = new MultiSphere(solid, 0.5f);
                break;
            case "rounded":
                collisionShape = new MultiSphere(solid);
                break;
            case "square":
                collisionShape = new HullCollisionShape(solid);
                break;
            default:
                message = "shapeName = " + MyString.quote(shapeName);
                throw new IllegalArgumentException(message);
        }
        /*
         * Add a rigid body with that shape.
         */
        rigidBody = new PhysicsRigidBody(collisionShape);
        rigidBody.setDebugMeshResolution(DebugShapeFactory.highResolution);
        PhysicsSpace space = bulletAppState.getPhysicsSpace();
        space.addCollisionObject(rigidBody);

        ++trialSeed;
    }
}
