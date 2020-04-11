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
package jme3utilities.minie.wizard;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.animation.BoneLink;
import com.jme3.bullet.animation.DacConfiguration;
import com.jme3.bullet.animation.DacLinks;
import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.bullet.animation.PhysicsLink;
import com.jme3.bullet.animation.RagUtils;
import com.jme3.bullet.animation.TorsoLink;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.PlaneCollisionShape;
import com.jme3.bullet.joints.Constraint;
import com.jme3.bullet.joints.JointEnd;
import com.jme3.bullet.joints.New6Dof;
import com.jme3.bullet.joints.SixDofJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Plane;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import java.util.List;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyAsset;
import jme3utilities.debug.AxesVisualizer;
import jme3utilities.debug.SkeletonVisualizer;
import jme3utilities.nifty.GuiScreenController;
import jme3utilities.ui.InputMode;

/**
 * The screen controller for the "test" screen of DacWizard.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class TestScreen extends GuiScreenController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final static Logger logger = Logger.getLogger(TestScreen.class.getName());
    // *************************************************************************
    // fields

    /**
     * debug material for the selected PhysicsLink
     */
    private Material selectMaterial;
    /**
     * horizontal plane added to physics space, or null if not added
     */
    private PhysicsRigidBody groundPlane = null;
    /**
     * root spatial of the C-G model being previewed
     */
    private Spatial viewedSpatial = null;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized, disabled screen that will not be enabled
     * during initialization.
     */
    TestScreen() {
        super("test", "Interface/Nifty/screens/wizard/test.xml", false);
    }
    // *************************************************************************
    // GuiScreenController methods

    /**
     * Initialize this (disabled) screen prior to its first update.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        super.initialize(stateManager, application);

        InputMode inputMode = InputMode.findMode("test");
        assert inputMode != null;
        setListener(inputMode);
        inputMode.influence(this);

        if (selectMaterial == null) {
            selectMaterial = MyAsset.createWireframeMaterial(assetManager,
                    ColorRGBA.White);
        }
    }

    /**
     * A callback from Nifty, invoked each time this screen shuts down.
     */
    @Override
    public void onEndScreen() {
        super.onEndScreen();
        removeGroundPlane();
    }

    /**
     * A callback from Nifty, invoked each time this screen starts up.
     */
    @Override
    public void onStartScreen() {
        super.onStartScreen();

        removeGroundPlane();
        DacWizard wizard = DacWizard.getApplication();
        wizard.clearScene();
        viewedSpatial = null;

        BulletAppState bulletAppState
                = DacWizard.findAppState(BulletAppState.class);
        bulletAppState.setDebugEnabled(true);
    }

    /**
     * Update this ScreenController prior to rendering. (Invoked once per
     * frame.)
     *
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (!hasStarted()) {
            return;
        }

        updateMarginButton();
        updateRagdollButton();
        updateViewButtons();

        Model model = DacWizard.getModel();
        Spatial nextSpatial = model.getRootSpatial();
        if (nextSpatial != viewedSpatial) {
            DacWizard wizard = DacWizard.getApplication();

            removeGroundPlane();
            wizard.clearScene();

            viewedSpatial = nextSpatial;
            if (nextSpatial != null) {
                Spatial cgModel = (Spatial) Heart.deepCopy(nextSpatial);
                Transform initTransform = model.copyInitTransform(null);
                cgModel.setLocalTransform(initTransform);
                wizard.makeScene(cgModel);
                addGroundPlane();

                AbstractControl control = RagUtils.findSControl(cgModel);
                Spatial controlledSpatial = control.getSpatial();

                DynamicAnimControl dac = model.copyRagdoll();
                controlledSpatial.addControl(dac);

                BulletAppState bulletAppState
                        = DacWizard.findAppState(BulletAppState.class);
                PhysicsSpace physicsSpace = bulletAppState.getPhysicsSpace();
                physicsSpace.add(dac);
            }
        }

        updateAxes();
        updateSelectedLink();
    }
    // *************************************************************************
    // private methods

    /**
     * If there isn't a ground plane, create one and add it to the PhysicsSpace.
     */
    private void addGroundPlane() {
        if (groundPlane == null) {
            Plane xzPlane = new Plane(Vector3f.UNIT_Y, 0f);
            PlaneCollisionShape shape = new PlaneCollisionShape(xzPlane);
            float mass = PhysicsRigidBody.massForStatic;
            groundPlane = new PhysicsRigidBody(shape, mass);

            BulletAppState bulletAppState
                    = DacWizard.findAppState(BulletAppState.class);
            PhysicsSpace physicsSpace = bulletAppState.getPhysicsSpace();
            physicsSpace.add(groundPlane);
        }
    }

    /**
     * Apply the pivot-to-PhysicsSpace transform of the specified Constraint to
     * the specified Spatial.
     */
    private void applyTransform(Constraint constraint, Spatial spatial) {
        Transform frame = new Transform(); // TODO garbage
        if (constraint instanceof New6Dof) {
            New6Dof new6dof = (New6Dof) constraint;
            new6dof.getFrameTransform(JointEnd.A, frame);
        } else {
            SixDofJoint sixDof = (SixDofJoint) constraint;
            sixDof.getFrameTransform(JointEnd.A, frame);
        }

        PhysicsRigidBody bodyA = constraint.getBodyA();
        Transform bodyTransform = bodyA.getTransform(null);
        bodyTransform.setScale(1f);
        frame.combineWithParent(bodyTransform);

        spatial.setLocalTransform(frame);
    }

    /**
     * Remove the ground plane (if any) from the PhysicsSpace.
     */
    private void removeGroundPlane() {
        if (groundPlane != null) {
            BulletAppState bulletAppState
                    = DacWizard.findAppState(BulletAppState.class);
            PhysicsSpace physicsSpace = bulletAppState.getPhysicsSpace();
            physicsSpace.remove(groundPlane);
            groundPlane = null;
        }
    }

    /**
     * Update the AxesVisualizer.
     */
    private void updateAxes() {
        DacWizard wizard = DacWizard.getApplication();
        AxesVisualizer axesVisualizer = wizard.findAxesVisualizer();
        Model model = DacWizard.getModel();
        String btName = model.selectedLink();

        if (viewedSpatial == null || btName.equals(DacLinks.torsoName)) {
            axesVisualizer.setEnabled(false);
        } else {
            /*
             * Align the visualizer axes with the PhysicsJoint.
             */
            DynamicAnimControl dac = wizard.findDac();
            PhysicsLink selectedLink = dac.findBoneLink(btName);
            Constraint constraint = (Constraint) selectedLink.getJoint();
            Spatial axesNode = axesVisualizer.getSpatial();
            applyTransform(constraint, axesNode);
            axesVisualizer.setEnabled(true);
        }
    }

    /**
     * Update the custom materials of the DAC's bodies.
     */
    private void updateSelectedLink() {
        DacWizard wizard = DacWizard.getApplication();
        DynamicAnimControl dac = wizard.findDac();
        if (dac != null) {
            Model model = DacWizard.getModel();
            String selectedBtName = model.selectedLink();

            List<BoneLink> boneLinks = dac.listLinks(BoneLink.class);
            for (BoneLink link : boneLinks) {
                String boneName = link.boneName();
                PhysicsRigidBody body = link.getRigidBody();
                if (boneName.equals(selectedBtName)) {
                    body.setDebugMaterial(selectMaterial);
                } else {
                    body.setDebugMaterial(null);
                }
            }
            TorsoLink link = dac.getTorsoLink();
            PhysicsRigidBody body = link.getRigidBody();
            if (selectedBtName.equals(DacConfiguration.torsoName)) {
                body.setDebugMaterial(selectMaterial);
            } else {
                body.setDebugMaterial(null);
            }
        }
    }

    /**
     * Update the collision-margin button.
     */
    private void updateMarginButton() {
        float margin = CollisionShape.getDefaultMargin();
        String marginButton = Float.toString(margin);
        setButtonText("margin", marginButton);
    }

    /**
     * Update the "Go limp" button.
     */
    private void updateRagdollButton() {
        DacWizard wizard = DacWizard.getApplication();
        DynamicAnimControl dac = wizard.findDac();

        String ragdollButton = "";
        if (dac != null && dac.isReady()) {
            TorsoLink torso = dac.getTorsoLink();
            if (torso.isKinematic()) {
                ragdollButton = "Go limp";
            } else {
                ragdollButton = "Reset model";
            }
        }
        setButtonText("ragdoll", ragdollButton);
    }

    /**
     * Update the buttons that toggle view elements.
     */
    private void updateViewButtons() {
        BulletAppState bulletAppState
                = DacWizard.findAppState(BulletAppState.class);

        String debugButton;
        if (bulletAppState.isDebugEnabled()) {
            debugButton = "Hide debug";
        } else {
            debugButton = "Show debug";
        }
        setButtonText("debug", debugButton);

        DacWizard app = DacWizard.getApplication();
        String meshButton;
        if (app.areMeshesHidden()) {
            meshButton = "Show meshes";
        } else {
            meshButton = "Hide meshes";
        }
        setButtonText("mesh", meshButton);

        String skeletonText = "";
        SkeletonVisualizer sv = app.findSkeletonVisualizer();
        if (sv != null) {
            if (sv.isEnabled()) {
                skeletonText = "Hide skeleton";
            } else {
                skeletonText = "Show skeleton";
            }
        }
        setButtonText("skeleton", skeletonText);
    }
}
