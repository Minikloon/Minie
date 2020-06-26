/*
 Copyright (c) 2019-2020, Stephen Gold
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
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.input.CameraInput;
import com.jme3.input.KeyInput;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.minie.test.common.AbstractDemo;
import jme3utilities.ui.CameraOrbitAppState;
import jme3utilities.ui.InputMode;
import jme3utilities.ui.Signals;

/**
 * Test/demonstrate rigid-body forces, torques, and impulses.
 * <p>
 * Collision objects are rendered entirely by debug visualization.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ForceDemo
        extends AbstractDemo
        implements PhysicsTickListener {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(ForceDemo.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = ForceDemo.class.getSimpleName();
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
     * subject body to which forces and torques are applied
     */
    private PhysicsRigidBody cube;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the ForceDemo application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        /*
         * Mute the chatty loggers in certain packages.
         */
        Heart.setLoggingLevels(Level.WARNING);

        Application application = new ForceDemo();
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
    // AbstractDemo methods

    /**
     * Initialize this application.
     */
    @Override
    public void actionInitializeApplication() {
        configureCamera();
        configureDumper();
        configurePhysics();

        ColorRGBA skyColor = new ColorRGBA(0.1f, 0.2f, 0.4f, 1f);
        viewPort.setBackgroundColor(skyColor);

        float length = 0.8f;
        attachWorldAxes(length);
        /*
         * Add the status text to the GUI.
         */
        statusText = new BitmapText(guiFont, false);
        statusText.setLocalTranslation(0f, cam.getHeight(), 0f);
        guiNode.attachChild(statusText);
        /*
         * Add a spinning cube.
         */
        BoxCollisionShape shape = new BoxCollisionShape(1f);
        float mass = 1f;
        cube = new PhysicsRigidBody(shape, mass);
        cube.setEnableSleep(false);
        Quaternion initialOrientation
                = new Quaternion().fromAngles(FastMath.HALF_PI, 0f, 0f);
        cube.setPhysicsRotation(initialOrientation);
        addCollisionObject(cube);
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
     * Determine the length of debug axis arrows when visible.
     *
     * @return the desired length (in physics-space units, &ge;0)
     */
    @Override
    protected float maxArrowLength() {
        return 2f;
    }

    /**
     * Add application-specific hotkey bindings and override existing ones.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();

        dim.bind(AbstractDemo.asDumpScene, KeyInput.KEY_P);
        dim.bind(AbstractDemo.asDumpSpace, KeyInput.KEY_O);

        dim.bind("signal " + CameraInput.FLYCAM_LOWER, KeyInput.KEY_DOWN);
        dim.bind("signal " + CameraInput.FLYCAM_RISE, KeyInput.KEY_UP);
        dim.bind("signal cf+Y", KeyInput.KEY_F3);
        dim.bind("signal cf-Y", KeyInput.KEY_F4);
        dim.bind("signal ci+Y", KeyInput.KEY_F7);
        dim.bind("signal ci-Y", KeyInput.KEY_F8);
        dim.bind("signal for+Y@+X", KeyInput.KEY_F6);
        dim.bind("signal imp+Y@+X", KeyInput.KEY_F9);
        dim.bind("signal orbitLeft", KeyInput.KEY_LEFT);
        dim.bind("signal orbitRight", KeyInput.KEY_RIGHT);
        dim.bind("signal torq+Y", KeyInput.KEY_F1);
        dim.bind("signal torq-Y", KeyInput.KEY_F2);

        dim.bind(AbstractDemo.asToggleAabbs, KeyInput.KEY_APOSTROPHE);
        dim.bind(AbstractDemo.asToggleHelp, KeyInput.KEY_H);
        dim.bind(AbstractDemo.asTogglePause, KeyInput.KEY_PAUSE);
        dim.bind(AbstractDemo.asTogglePause, KeyInput.KEY_PERIOD);
        dim.bind(AbstractDemo.asTogglePcoAxes, KeyInput.KEY_SEMICOLON);
        dim.bind(AbstractDemo.asToggleVArrows, KeyInput.KEY_K);

        float margin = 10f; // in pixels
        float width = cam.getWidth() - 2f * margin;
        float height = cam.getHeight() - (2f * margin + 20f);
        float leftX = margin;
        float topY = height + margin;
        Rectangle rectangle = new Rectangle(leftX, topY, width, height);

        attachHelpNode(rectangle);
    }

    /**
     * Callback invoked once per frame.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);
        updateStatusText();
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
        /*
         * Check UI signals and apply forces/torques accordingly.
         */
        Signals signals = getSignals();

        if (signals.test("cf+Y")) {
            cube.applyCentralForce(new Vector3f(0f, 1f, 0f));
        }
        if (signals.test("cf-Y")) {
            cube.applyCentralForce(new Vector3f(0f, -1f, 0f));
        }
        if (signals.test("ci+Y")) {
            cube.applyCentralImpulse(new Vector3f(0f, 0.1f, 0f));
        }
        if (signals.test("ci-Y")) {
            cube.applyCentralImpulse(new Vector3f(0f, -0.1f, 0f));
        }
        if (signals.test("for+Y@+X")) {
            cube.applyForce(new Vector3f(0f, 1f, 0f),
                    new Vector3f(1f, 0f, 0f));
        }
        if (signals.test("imp+Y@+X")) {
            cube.applyImpulse(new Vector3f(0f, 0.1f, 0f),
                    new Vector3f(1f, 0f, 0f));
        }
        if (signals.test("torq+Y")) {
            cube.applyTorque(new Vector3f(0f, 1f, 0f));
        }
        if (signals.test("torq-Y")) {
            cube.applyTorque(new Vector3f(0f, -1f, 0f));
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
     * Configure the camera during startup.
     */
    private void configureCamera() {
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(4f);
        flyCam.setZoomSpeed(4f);

        cam.setLocation(new Vector3f(2.65f, 2.42f, 9.37f));
        cam.setRotation(new Quaternion(0f, 0.9759f, -0.04f, -0.2136f));

        CameraOrbitAppState orbitState
                = new CameraOrbitAppState(cam, "orbitLeft", "orbitRight");
        stateManager.attach(orbitState);
    }

    /**
     * Configure physics during startup.
     */
    private void configurePhysics() {
        bulletAppState = new BulletAppState();
        float axisLength = maxArrowLength();
        bulletAppState.setDebugAxisLength(axisLength);
        bulletAppState.setDebugEnabled(true);
        stateManager.attach(bulletAppState);

        PhysicsSpace space = getPhysicsSpace();
        space.addTickListener(this);
        setGravityAll(0f);
    }

    /**
     * Update the status text in the GUI.
     */
    private void updateStatusText() {
        float v = cube.getLinearVelocity().length();
        float omega = cube.getAngularVelocity().length();
        String message = String.format(" v=%f psu/s, omega=%f rad/s", v, omega);
        statusText.setText(message);
    }
}
