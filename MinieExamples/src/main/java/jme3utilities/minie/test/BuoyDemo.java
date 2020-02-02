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

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.Application;
import com.jme3.app.StatsAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.animation.CenterHeuristic;
import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.bullet.animation.LinkConfig;
import com.jme3.bullet.animation.MassHeuristic;
import com.jme3.bullet.animation.PhysicsLink;
import com.jme3.bullet.animation.RagUtils;
import com.jme3.bullet.animation.ShapeHeuristic;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.font.Rectangle;
import com.jme3.input.CameraInput;
import com.jme3.input.KeyInput;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;
import com.jme3.water.SimpleWaterProcessor;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.InfluenceUtil;
import jme3utilities.Misc;
import jme3utilities.MySpatial;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.math.MyVector3f;
import jme3utilities.minie.DumpFlags;
import jme3utilities.minie.FilterAll;
import jme3utilities.minie.PhysicsDumper;
import jme3utilities.minie.test.controllers.BuoyController;
import jme3utilities.minie.test.tunings.CesiumManControl;
import jme3utilities.minie.test.tunings.ElephantControl;
import jme3utilities.minie.test.tunings.JaimeControl;
import jme3utilities.minie.test.tunings.MhGameControl;
import jme3utilities.minie.test.tunings.NinjaControl;
import jme3utilities.minie.test.tunings.OtoControl;
import jme3utilities.minie.test.tunings.PuppetControl;
import jme3utilities.minie.test.tunings.SinbadControl;
import jme3utilities.ui.ActionApplication;
import jme3utilities.ui.HelpUtils;
import jme3utilities.ui.InputMode;
import jme3utilities.ui.Signals;

/**
 * Demonstrate BuoyController.
 * <p>
 * Seen in the March 2019 demo video:
 * https://www.youtube.com/watch?v=eq09m7pbk5A
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class BuoyDemo extends ActionApplication {
    // *************************************************************************
    // constants and loggers

    /**
     * Y coordinate of the water's surface (in world coordinates)
     */
    final public static float surfaceElevation = 0f;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(BuoyDemo.class.getName());
    /**
     * application name (for the title bar of the app's window)
     */
    final private static String applicationName
            = BuoyDemo.class.getSimpleName();
    // *************************************************************************
    // fields

    /**
     * AppState to manage the PhysicsSpace
     */
    final private BulletAppState bulletAppState = new BulletAppState();
    /**
     * Control being tested
     */
    private DynamicAnimControl dac;
    /**
     * filter to control visualization of axis-aligned bounding boxes
     */
    private FilterAll bbFilter;
    /**
     * root node of the C-G model on which the Control is being tested
     */
    private Node cgModel;
    /**
     * GUI node for displaying hotkey help/hints
     */
    private Node helpNode;
    /**
     * scene-graph subtree containing all geometries visible in reflections
     */
    final private Node reflectiblesNode = new Node("reflectibles");
    /**
     * scene-graph subtree containing all reflective geometries
     */
    final private Node reflectorsNode = new Node("reflectors");
    /**
     * dump debugging information to System.out
     */
    final private PhysicsDumper dumper = new PhysicsDumper();
    /**
     * space for physics simulation
     */
    private PhysicsSpace physicsSpace;
    /**
     * scene processor for water effects
     */
    private SimpleWaterProcessor processor;
    /**
     * SkeletonControl of the loaded model
     */
    private SkeletonControl sc;
    /**
     * visualizer for the skeleton of the C-G model
     */
    private SkeletonVisualizer sv;
    /**
     * name of the Animation to play on the C-G model
     */
    private String animationName = null;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the BuoyDemo application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        /*
         * Mute the chatty loggers in certain packages.
         */
        Misc.setLoggingLevels(Level.WARNING);

        Application application = new BuoyDemo();
        /*
         * Customize the window's title bar.
         */
        AppSettings settings = new AppSettings(true);
        settings.setTitle(applicationName);

        settings.setSamples(4); // anti-aliasing
        settings.setVSync(true);
        application.setSettings(settings);

        application.start();
    }
    // *************************************************************************
    // ActionApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void actionInitializeApplication() {
        rootNode.attachChild(reflectiblesNode);
        rootNode.attachChild(reflectorsNode);

        configureCamera();
        configureDumper();
        configurePhysics();
        addLighting();
        /*
         * Hide the render-statistics overlay.
         */
        stateManager.getState(StatsAppState.class).toggleStats();

        addSurface();
        addSky();
        addModel("Sinbad");
    }

    /**
     * Add application-specific hotkey bindings and override existing ones.
     */
    @Override
    public void moreDefaultBindings() {
        InputMode dim = getDefaultInputMode();

        dim.bind("dump physicsSpace", KeyInput.KEY_O);
        dim.bind("dump scenes", KeyInput.KEY_P);
        dim.bind("go floating", KeyInput.KEY_0);
        dim.bind("go floating", KeyInput.KEY_SPACE);

        //dim.bind("load CesiumMan", KeyInput.KEY_F12);
        dim.bind("load Elephant", KeyInput.KEY_F3);
        dim.bind("load Jaime", KeyInput.KEY_F2);
        dim.bind("load MhGame", KeyInput.KEY_F9);
        dim.bind("load Ninja", KeyInput.KEY_F7);
        dim.bind("load Oto", KeyInput.KEY_F6);
        dim.bind("load Puppet", KeyInput.KEY_F8);
        dim.bind("load Sinbad", KeyInput.KEY_F1);
        dim.bind("load SinbadWith1Sword", KeyInput.KEY_F10);
        dim.bind("load SinbadWithSwords", KeyInput.KEY_F4);

        dim.bind("signal " + CameraInput.FLYCAM_LOWER, KeyInput.KEY_DOWN);
        dim.bind("signal " + CameraInput.FLYCAM_RISE, KeyInput.KEY_UP);
        dim.bind("signal rotateLeft", KeyInput.KEY_LEFT);
        dim.bind("signal rotateRight", KeyInput.KEY_RIGHT);

        dim.bind("toggle aabb", KeyInput.KEY_APOSTROPHE);
        dim.bind("toggle axes", KeyInput.KEY_SEMICOLON);
        dim.bind("toggle help", KeyInput.KEY_H);
        dim.bind("toggle meshes", KeyInput.KEY_M);
        dim.bind("toggle pause", KeyInput.KEY_PERIOD);
        dim.bind("toggle physics debug", KeyInput.KEY_SLASH);
        dim.bind("toggle skeleton", KeyInput.KEY_V);

        float x = 10f;
        float y = cam.getHeight() - 10f;
        float width = cam.getWidth() - 20f;
        float height = cam.getHeight() - 20f;
        Rectangle rectangle = new Rectangle(x, y, width, height);

        float space = 20f;
        helpNode = HelpUtils.buildNode(dim, rectangle, guiFont, space);
        guiNode.attachChild(helpNode);
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
            switch (actionString) {
                case "dump physicsSpace":
                    dumper.dump(physicsSpace);
                    return;
                case "dump scenes":
                    dumper.dump(renderManager);
                    return;

                case "go floating":
                    goFloating();
                    return;

                case "toggle aabb":
                    toggleAabb();
                    return;
                case "toggle axes":
                    toggleAxes();
                    return;
                case "toggle help":
                    toggleHelp();
                    return;
                case "toggle meshes":
                    toggleMeshes();
                    return;
                case "toggle pause":
                    togglePause();
                    return;
                case "toggle physics debug":
                    togglePhysicsDebug();
                    return;
                case "toggle skeleton":
                    toggleSkeleton();
                    return;
            }
            String[] words = actionString.split(" ");
            if (words.length == 2 && "load".equals(words[0])) {
                addModel(words[1]);
                return;
            }
        }
        super.onAction(actionString, ongoing, tpf);
    }

    /**
     * Callback invoked once per frame.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);

        Signals signals = getSignals();

        float rotateAngle = 0f;
        if (signals.test("rotateRight")) {
            rotateAngle += tpf;
        }
        if (signals.test("rotateLeft")) {
            rotateAngle -= tpf;
        }
        if (rotateAngle != 0f) {
            rotateAngle /= speed;
            Quaternion orientation = MySpatial.worldOrientation(cgModel, null);
            Quaternion rotate = new Quaternion();
            rotate.fromAngles(0f, rotateAngle, 0f);
            rotate.mult(orientation, orientation);
            MySpatial.setWorldOrientation(cgModel, orientation);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Add lighting and reflections to the scene.
     */
    private void addLighting() {
        ColorRGBA ambientColor = new ColorRGBA(0.2f, 0.2f, 0.2f, 1f);
        AmbientLight ambient = new AmbientLight(ambientColor);
        rootNode.addLight(ambient);

        Vector3f direction = new Vector3f(-1f, -0.2f, -1f).normalizeLocal();
        DirectionalLight sun = new DirectionalLight(direction);
        rootNode.addLight(sun);

        processor = new SimpleWaterProcessor(assetManager);
        viewPort.addProcessor(processor);
        processor.setLightPosition(direction.mult(200f));
        Plane surface = new Plane(Vector3f.UNIT_Y, surfaceElevation);
        processor.setPlane(surface);
        processor.setReflectionScene(reflectiblesNode);
        /*
         * Clip everything below the surface.
         */
        processor.setReflectionClippingOffset(-0.1f);
        /*
         * Configure water and wave parameters.
         */
        float waveHeight = 0.2f;
        processor.setDistortionScale(waveHeight);
        float waterTransparency = 0.4f;
        processor.setWaterDepth(waterTransparency);
        float waveSpeed = 0.06f;
        processor.setWaveSpeed(waveSpeed);
    }

    /**
     * Add an animated model to the scene, removing any previously added model.
     *
     * @param modelName the name of the model to add (not null, not empty)
     */
    private void addModel(String modelName) {
        if (cgModel != null) {
            dac.getSpatial().removeControl(dac);
            reflectiblesNode.detachChild(cgModel);
            rootNode.removeControl(sv);
        }

        switch (modelName) {
            case "CesiumMan":
                loadCesiumMan();
                break;
            case "Elephant":
                loadElephant();
                break;
            case "Jaime":
                loadJaime();
                break;
            case "MhGame":
                loadMhGame();
                break;
            case "Ninja":
                loadNinja();
                break;
            case "Oto":
                loadOto();
                break;
            case "Puppet":
                loadPuppet();
                break;
            case "Sinbad":
                loadSinbad();
                break;
            case "SinbadWith1Sword":
                loadSinbadWith1Sword();
                break;
            case "SinbadWithSwords":
                loadSinbadWithSwords();
                break;
            default:
                throw new IllegalArgumentException(modelName);
        }

        List<Spatial> list = MySpatial.listSpatials(cgModel);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }
        cgModel.setCullHint(Spatial.CullHint.Never);

        reflectiblesNode.attachChild(cgModel);
        setHeight(cgModel, 2f);
        center(cgModel);

        sc = RagUtils.findSkeletonControl(cgModel);
        Spatial controlledSpatial = sc.getSpatial();

        controlledSpatial.addControl(dac);
        dac.setPhysicsSpace(physicsSpace);
        /*
         * Add buoyancy to each BoneLink.
         */
        List<PhysicsLink> links = dac.listLinks(PhysicsLink.class);
        float density = 1.5f;
        for (PhysicsLink link : links) {
            BuoyController buoy
                    = new BuoyController(link, density, surfaceElevation);
            link.addIKController(buoy);
        }

        AnimControl animControl
                = controlledSpatial.getControl(AnimControl.class);
        AnimChannel animChannel = animControl.createChannel();
        animChannel.setAnim(animationName);

        sv = new SkeletonVisualizer(assetManager, sc);
        sv.setLineColor(ColorRGBA.Yellow);
        if (sc instanceof SkeletonControl) {
            /*
             * Clean up Jaime's skeleton visualization by hiding the "IK" bones,
             * which don't influence any mesh vertices.
             */
            InfluenceUtil.hideNonInfluencers(sv, (SkeletonControl) sc);
        }
        rootNode.addControl(sv);
    }

    /**
     * Add a cube-mapped sky.
     */
    private void addSky() {
        String assetPath = "Textures/Sky/Bright/BrightSky.dds";
        Spatial sky = SkyFactory.createSky(assetManager, assetPath,
                SkyFactory.EnvMapType.CubeMap);
        reflectiblesNode.attachChild(sky);
    }

    /**
     * Add a large Quad to represent the surface of the water.
     */
    private void addSurface() {
        float diameter = 400f;
        Mesh mesh = new Quad(diameter, diameter);
        mesh.scaleTextureCoordinates(new Vector2f(80f, 80f));

        Geometry geometry = new Geometry("water surface", mesh);
        reflectorsNode.attachChild(geometry);

        geometry.move(-diameter / 2, 0f, diameter / 2);
        Quaternion rot = new Quaternion();
        rot.fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);
        geometry.setLocalRotation(rot);
        Material material = processor.getMaterial();
        geometry.setMaterial(material);
    }

    /**
     * Translate a model's center so that the model rests on the X-Z plane, and
     * its center lies on the Y axis.
     */
    private void center(Spatial model) {
        Vector3f[] minMax = MySpatial.findMinMaxCoords(model);
        Vector3f center = MyVector3f.midpoint(minMax[0], minMax[1], null);
        Vector3f offset = new Vector3f(center.x, minMax[0].y, center.z);

        Vector3f location = model.getWorldTranslation();
        location.subtractLocal(offset);
        MySpatial.setWorldLocation(model, location);
    }

    /**
     * Configure the camera during startup.
     */
    private void configureCamera() {
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(4f);

        cam.setLocation(new Vector3f(-0.8f, 3f, 6f));
        cam.setRotation(new Quaternion(0.008f, 0.9874f, -0.1482f, 0.055f));
    }

    /**
     * Configure the PhysicsDumper during startup.
     */
    private void configureDumper() {
        dumper.setEnabled(DumpFlags.JointsInBodies, true);
        dumper.setEnabled(DumpFlags.JointsInSpaces, true);
        dumper.setEnabled(DumpFlags.Transforms, true);
    }

    /**
     * Configure physics during startup.
     */
    private void configurePhysics() {
        CollisionShape.setDefaultMargin(0.005f); // 5-mm margin
        stateManager.attach(bulletAppState);

        physicsSpace = bulletAppState.getPhysicsSpace();
        physicsSpace.setAccuracy(0.01f); // 10-msec timestep
        physicsSpace.setSolverNumIterations(15);
    }

    /**
     * Put the loaded model into ragdoll mode with buoyancy enabled.
     */
    private void goFloating() {
        if (dac.isReady()) {
            dac.setRagdollMode();
        }
    }

    /**
     * Load the CesiumMan model (not included due to licensing issues).
     */
    private void loadCesiumMan() {
        cgModel = (Node) assetManager.loadModel(
                "Models/CesiumMan/glTF-Binary/CesiumMan.glb");
        cgModel.rotate(0f, -1.6f, 0f);
        dac = new CesiumManControl();
        animationName = "anim_0";
    }

    /**
     * Load the Elephant model.
     */
    private void loadElephant() {
        cgModel = (Node) assetManager.loadModel("Models/Elephant/Elephant.j3o");
        cgModel.rotate(0f, 1.6f, 0f);
        dac = new ElephantControl();
        animationName = "legUp";
    }

    /**
     * Load the Jaime model.
     */
    private void loadJaime() {
        cgModel = (Node) assetManager.loadModel("Models/Jaime/Jaime.j3o");
        Geometry g = (Geometry) cgModel.getChild(0);
        RenderState rs = g.getMaterial().getAdditionalRenderState();
        rs.setFaceCullMode(RenderState.FaceCullMode.Off);

        dac = new JaimeControl();
        animationName = "Punches";
    }

    /**
     * Load the MhGame model.
     */
    private void loadMhGame() {
        cgModel = (Node) assetManager.loadModel("Models/MhGame/MhGame.j3o");
        dac = new MhGameControl();
        animationName = "expr-lib-pose";
    }

    /**
     * Load the Ninja model.
     */
    private void loadNinja() {
        cgModel = (Node) assetManager.loadModel("Models/Ninja/Ninja.j3o");
        cgModel.rotate(0f, 3f, 0f);
        dac = new NinjaControl();
        animationName = "Walk";
    }

    /**
     * Load the Oto model.
     */
    private void loadOto() {
        cgModel = (Node) assetManager.loadModel("Models/Oto/Oto.j3o");
        dac = new OtoControl();
        animationName = "Walk";
    }

    /**
     * Load the Puppet model.
     */
    private void loadPuppet() {
        cgModel = (Node) assetManager.loadModel("Models/Puppet/Puppet.j3o");
        dac = new PuppetControl();
        animationName = "walk";
    }

    /**
     * Load the Sinbad model without attachments.
     */
    private void loadSinbad() {
        cgModel = (Node) assetManager.loadModel("Models/Sinbad/Sinbad.j3o");
        dac = new SinbadControl();
        animationName = "Dance";
    }

    /**
     * Load the Sinbad model with an attached sword.
     */
    private void loadSinbadWith1Sword() {
        cgModel = (Node) assetManager.loadModel("Models/Sinbad/Sinbad.j3o");

        Node sword = (Node) assetManager.loadModel("Models/Sinbad/Sword.j3o");
        List<Spatial> list = MySpatial.listSpatials(sword);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }

        LinkConfig swordConfig = new LinkConfig(5f, MassHeuristic.Density,
                ShapeHeuristic.VertexHull, Vector3f.UNIT_XYZ,
                CenterHeuristic.AABB);
        dac = new SinbadControl();
        dac.attach("Handle.R", swordConfig, sword);

        animationName = "IdleTop";
    }

    /**
     * Load the Sinbad model with 2 attached swords.
     */
    private void loadSinbadWithSwords() {
        cgModel = (Node) assetManager.loadModel("Models/Sinbad/Sinbad.j3o");

        Node sword = (Node) assetManager.loadModel("Models/Sinbad/Sword.j3o");
        List<Spatial> list = MySpatial.listSpatials(sword);
        for (Spatial spatial : list) {
            spatial.setShadowMode(RenderQueue.ShadowMode.Cast);
        }

        LinkConfig swordConfig = new LinkConfig(5f, MassHeuristic.Density,
                ShapeHeuristic.VertexHull, Vector3f.UNIT_XYZ,
                CenterHeuristic.AABB);
        dac = new SinbadControl();
        dac.attach("Handle.L", swordConfig, sword);
        dac.attach("Handle.R", swordConfig, sword);

        animationName = "RunTop";
    }

    /**
     * Scale the specified model uniformly so that it has the specified height.
     *
     * @param model (not null, modified)
     * @param height (in world units)
     */
    private void setHeight(Spatial model, float height) {
        Vector3f[] minMax = MySpatial.findMinMaxCoords(model);
        float oldHeight = minMax[1].y - minMax[0].y;

        model.scale(height / oldHeight);
    }

    /**
     * Toggle visualization of collision-object bounding boxes.
     */
    private void toggleAabb() {
        if (bbFilter == null) {
            bbFilter = new FilterAll(true);
        } else {
            bbFilter = null;
        }

        bulletAppState.setDebugBoundingBoxFilter(bbFilter);
    }

    /**
     * Toggle visualization of collision-object axes.
     */
    private void toggleAxes() {
        float length = bulletAppState.debugAxisLength();
        bulletAppState.setDebugAxisLength(0.5f - length);
    }

    /**
     * Toggle visibility of the helpNode.
     */
    private void toggleHelp() {
        if (helpNode.getCullHint() == Spatial.CullHint.Always) {
            helpNode.setCullHint(Spatial.CullHint.Never);
        } else {
            helpNode.setCullHint(Spatial.CullHint.Always);
        }
    }

    /**
     * Toggle mesh rendering on/off.
     */
    private void toggleMeshes() {
        Spatial.CullHint hint = cgModel.getLocalCullHint();
        if (hint == Spatial.CullHint.Inherit
                || hint == Spatial.CullHint.Never) {
            hint = Spatial.CullHint.Always;
        } else if (hint == Spatial.CullHint.Always) {
            hint = Spatial.CullHint.Never;
        }
        cgModel.setCullHint(hint);
    }

    /**
     * Toggle the animation and physics simulation: paused/running.
     */
    private void togglePause() {
        float newSpeed = (speed > 1e-12f) ? 1e-12f : 1f;
        setSpeed(newSpeed);
    }

    /**
     * Toggle physics-debug visualization on/off.
     */
    private void togglePhysicsDebug() {
        boolean enabled = bulletAppState.isDebugEnabled();
        bulletAppState.setDebugEnabled(!enabled);
    }

    /**
     * Toggle the skeleton visualizer on/off.
     */
    private void toggleSkeleton() {
        boolean enabled = sv.isEnabled();
        sv.setEnabled(!enabled);
    }
}
