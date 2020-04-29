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

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimComposer;
import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.animation.CenterHeuristic;
import com.jme3.bullet.animation.DacConfiguration;
import com.jme3.bullet.animation.DynamicAnimControl;
import com.jme3.bullet.animation.LinkConfig;
import com.jme3.bullet.animation.MassHeuristic;
import com.jme3.bullet.animation.RagUtils;
import com.jme3.bullet.animation.RangeOfMotion;
import com.jme3.bullet.animation.ShapeHeuristic;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.InfluenceUtil;
import jme3utilities.MyAnimation;
import jme3utilities.MySpatial;
import jme3utilities.math.MyVector3f;
import jme3utilities.math.VectorSet;
import jme3utilities.ui.InputMode;
import jme3utilities.ui.Locators;

/**
 * State information in the DacWizard application.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class Model {
    // *************************************************************************
    // constants and loggers

    /**
     * desired height of the C-G model (for visualization, in world units)
     */
    final public static float cgmHeight = 10f;
    /**
     * message logger for this class
     */
    final static Logger logger
            = Logger.getLogger(Model.class.getName());
    // *************************************************************************
    // fields

    /**
     * bones that influence the Mesh in any way
     */
    private BitSet anyInfluenceBones;
    /**
     * bones that directly influence the Mesh
     */
    private BitSet directInfluenceBones;
    /**
     * bones that will be linked
     */
    private BitSet linkedBones;
    /**
     * whether to show PhysicsJoint axes in the TestScreen
     */
    private boolean isShowingAxes = false;
    /**
     * PhysicsControl that will be added to the C-G model
     */
    private DynamicAnimControl ragdoll;
    /**
     * Exception that occurred during load
     */
    private Exception loadException;
    /**
     * task for estimating ranges of motion
     */
    private FutureTask<RangeOfMotion[]> romTask;
    /**
     * number of components in the file-system path to the asset root
     */
    private int numComponentsInRoot;
    /**
     * map manager names to sets of vertices
     */
    private Map<String, VectorSet> coordsMap;
    /**
     * callable for estimating ranges of motion
     */
    private RomCallable romCallable;
    /**
     * root spatial of the C-G model
     */
    private Spatial rootSpatial;
    /**
     * bone/torso name of the selected PhysicsLink
     */
    private String selectedLink;
    /**
     * components of the file-system path to the C-G model (not null)
     */
    private String[] filePathComponents = new String[0];
    /**
     * initial Transform for visualization
     */
    final private Transform initTransform = new Transform();
    // *************************************************************************
    // new methods exposed

    /**
     * Determine the asset path to the J3O binary. The file-system path must be
     * set.
     *
     * @return the path (not null, not empty)
     */
    String assetPath() {
        int numComponents = filePathComponents.length;
        if (numComponents == 0) {
            throw new RuntimeException("File-system path not set.");
        }
        assert numComponentsInRoot < numComponents : numComponents;
        String[] resultComponents = Arrays.copyOfRange(filePathComponents,
                numComponentsInRoot, numComponents);
        String result = String.join("/", resultComponents);
        result = "/" + result;

        assert result != null;
        assert !result.isEmpty();
        return result;
    }

    /**
     * Determine the file-system path to the asset root. The file-system path
     * must be set.
     *
     * @return the path (not null, not empty)
     */
    String assetRoot() {
        int numCompoments = filePathComponents.length;
        if (numCompoments == 0) {
            throw new RuntimeException("File-system path not set.");
        }
        assert numComponentsInRoot < numCompoments : numCompoments;
        String[] resultComponents = Arrays.copyOfRange(filePathComponents, 0,
                numComponentsInRoot);
        String result = String.join("/", resultComponents);
        result += "/";

        assert result != null;
        assert !result.isEmpty();
        return result;
    }

    /**
     * Read the name of the indexed bone. A C-G model must be loaded.
     *
     * @param boneIndex which bone (&ge;0)
     * @return the name (may be null)
     */
    String boneName(int boneIndex) {
        if (rootSpatial == null) {
            throw new RuntimeException("No model loaded.");
        }

        String result;
        Skeleton skeleton = findSkeleton();
        if (skeleton == null) {
            Armature armature = findArmature();
            Joint joint = armature.getJoint(boneIndex);
            result = joint.getName();

        } else {
            Bone bone = skeleton.getBone(boneIndex);
            result = bone.getName();
        }

        return result;
    }

    /**
     * Read the configuration of the named bone/torso link.
     *
     * @param boneName the name of the bone/torso (not null)
     * @return the pre-existing configuration (not null)
     */
    LinkConfig config(String boneName) {
        LinkConfig result = ragdoll.config(boneName);
        return result;
    }

    /**
     * Copy the initial Transform for visualization.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return a Transform relative to world coordinates (either storeResult or
     * a new instance)
     */
    Transform copyInitTransform(Transform storeResult) {
        if (storeResult == null) {
            return initTransform.clone();
        } else {
            return storeResult.set(initTransform);
        }
    }

    /**
     * Copy the configured DynamicAnimControl.
     *
     * @return a new Control, or null if no model loaded
     */
    DynamicAnimControl copyRagdoll() {
        DynamicAnimControl clone = (DynamicAnimControl) Heart.deepCopy(ragdoll);
        return clone;
    }

    /**
     * Count how many bones are in the skeleton. A C-G model must be loaded.
     *
     * @return the count (&ge;0)
     */
    int countBones() {
        if (rootSpatial == null) {
            throw new RuntimeException("No model loaded.");
        }

        int count = 0;
        Skeleton skeleton = findSkeleton();
        if (skeleton != null) {
            count = skeleton.getBoneCount();
        } else {
            Armature armature = findArmature();
            if (armature != null) {
                count = armature.getJointCount();
            }
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count how many dynamic anim controls are in the model.
     *
     * @return the count (&ge;0) or 0 if no model loaded
     */
    int countDacs() {
        int count = MySpatial.countControls(rootSpatial,
                DynamicAnimControl.class);

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count how many bones are managed by the specified bone/torso link. A C-G
     * model must be loaded.
     *
     * @param managerName the bone/torso name of the manager (not null)
     * @return the count (&ge;0)
     */
    int countManagedBones(String managerName) {
        if (rootSpatial == null) {
            throw new RuntimeException("No model loaded.");
        }

        int count = 0;

        Skeleton skeleton = findSkeleton();
        if (skeleton == null) {
            Armature armature = findArmature();
            int numJoints = armature.getJointCount();
            for (int boneIndex = 0; boneIndex < numJoints; ++boneIndex) {
                Joint joint = armature.getJoint(boneIndex);
                String name = findManager(joint, armature);
                if (managerName.equals(name)) {
                    ++count;
                }
            }

        } else {
            int numBones = skeleton.getBoneCount();
            for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
                Bone bone = skeleton.getBone(boneIndex);
                String name = findManager(bone, skeleton);
                if (managerName.equals(name)) {
                    ++count;
                }
            }
        }

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count how many skeleton controls are in the model.
     *
     * @return the count (&ge;0) or 0 if no model loaded
     */
    int countSkeletonControls() {
        int count = MySpatial.countControls(rootSpatial, SkeletonControl.class);
        count += MySpatial.countControls(rootSpatial, SkinningControl.class);

        assert count >= 0 : count;
        return count;
    }

    /**
     * Count how many tracks in the C-G model use the indexed bone. A C-G model
     * must be loaded.
     *
     * @param boneIndex which bone (&ge;0)
     * @return the count (&ge;0)
     */
    int countTracks(int boneIndex) {
        if (rootSpatial == null) {
            throw new RuntimeException("No model loaded.");
        }

        int count = 0;

        List<AnimComposer> composers
                = MySpatial.listControls(rootSpatial, AnimComposer.class, null);
        for (AnimComposer composer : composers) {
            Collection<String> clipNames = composer.getAnimClipsNames();
            for (String clipName : clipNames) {
                AnimClip clip = composer.getAnimClip(clipName);
                if (MyAnimation.findTransformTrack(clip, boneIndex) != null) {
                    ++count;
                }
            }
        }

        List<AnimControl> animControls
                = MySpatial.listControls(rootSpatial, AnimControl.class, null);
        for (AnimControl animControl : animControls) {
            Collection<String> animationNames = animControl.getAnimationNames();
            for (String animName : animationNames) {
                Animation animation = animControl.getAnim(animName);
                if (MyAnimation.hasTrackForBone(animation, boneIndex)) {
                    ++count;
                }
            }
        }

        return count;
    }

    /**
     * Count how many vertices would be assigned to the named bone/torso.
     *
     * @param boneName (not null)
     * @return the count (&ge;0)
     */
    int countVertices(String boneName) {
        int count;
        VectorSet vertices = coordsMap.get(boneName);
        if (vertices == null) {
            count = 0;
        } else {
            count = vertices.numVectors();
        }

        return count;
    }

    /**
     * Describe the influence of the indexed bone in the loaded C-G model.
     *
     * @param boneIndex which bone (&ge;0)
     * @return descriptive text (not null, not empty)
     */
    String describeBoneInfluence(int boneIndex) {
        String result;
        if (directInfluenceBones.get(boneIndex)) {
            result = "has direct mesh influence";
        } else if (anyInfluenceBones.get(boneIndex)) {
            result = "mesh influence (indirect only)";
        } else {
            result = "NO mesh influence";
        }

        return result;
    }

    /**
     * Determine the file-system path to the J3O binary.
     *
     * @return the path (not null, may be empty)
     */
    String filePath() {
        String result = String.join("/", filePathComponents);
        assert result != null;
        return result;
    }

    /**
     * Access the root spatial of the loaded C-G model.
     *
     * @return the pre-existing spatial, or null if no model loaded
     */
    Spatial getRootSpatial() {
        return rootSpatial;
    }

    /**
     * Test whether there's a pre-existing DynamicAnimControl with the exact
     * same set of linked bones.
     *
     * @return true if a matching Control exists, otherwise false
     */
    boolean hasConfiguredRagdoll() {
        if (ragdoll == null) {
            return false;
        }

        int numBones = countBones();
        boolean result = true;
        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            String name = boneName(boneIndex);
            boolean isLinked = ragdoll.hasBoneLink(name);
            if (isLinked != linkedBones.get(boneIndex)) {
                result = false;
                break;
            }
        }

        return result;
    }

    /**
     * Test whether the indexed bone will be linked.
     *
     * @param boneIndex which bone (&ge;0)
     * @return true if linked, otherwise false
     */
    boolean isBoneLinked(int boneIndex) {
        boolean result = linkedBones.get(boneIndex);
        return result;
    }

    /**
     * Test whether the PhysicsJoint axes will be rendered.
     *
     * @return true if rendered, otherwise false
     */
    boolean isShowingAxes() {
        return isShowingAxes;
    }

    /**
     * Determine the parent (in the link hierarchy) of the named linked bone. A
     * C-G model must be loaded.
     *
     * @param childName the bone name of the child (not null, not empty)
     * @return the bone/torso name of the parent
     */
    String linkedBoneParentName(String childName) {
        assert childName != null;
        assert !childName.isEmpty();
        if (rootSpatial == null) {
            throw new RuntimeException("No model loaded.");
        }

        String name;
        Skeleton skeleton = findSkeleton();
        if (skeleton == null) {
            Armature armature = findArmature();
            Joint child = armature.getJoint(childName);
            Joint parent = child.getParent();
            if (parent == null) { // the named Joint was a root joint
                name = DacConfiguration.torsoName;
            } else {
                name = findManager(parent, armature);
            }

        } else {
            Bone child = skeleton.getBone(childName);
            Bone parent = child.getParent();
            if (parent == null) { // the named Bone was a root bone
                name = DacConfiguration.torsoName;
            } else {
                name = findManager(parent, skeleton);
            }
        }

        return name;
    }

    /**
     * Enumerate the indices of all bones that will be linked. A C-G model must
     * be loaded.
     *
     * @return a new array of indices (not null)
     */
    int[] listLinkedBones() {
        if (rootSpatial == null) {
            throw new RuntimeException("No model loaded.");
        }

        int numBones = countBones();
        int numLinkedBones = linkedBones.cardinality();
        int[] result = new int[numLinkedBones];
        int linkedBoneIndex = 0;
        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            if (linkedBones.get(boneIndex)) {
                result[linkedBoneIndex] = boneIndex;
                ++linkedBoneIndex;
            }
        }

        return result;
    }

    /**
     * Attempt to load a C-G model. The file-system path must have been
     * previously set. If successful, rootSpatial and linkedBones are
     * initialized. Otherwise, rootSpatial==null and loadException is set.
     */
    void load() {
        int numComponents = filePathComponents.length;
        if (numComponents == 0) {
            throw new RuntimeException("File-system path not set.");
        }

        unload();
        String assetRoot = assetRoot();
        String assetPath = assetPath();

        Locators.save();
        Locators.unregisterAll();
        Locators.registerFilesystem(assetRoot);
        Locators.registerDefault();
        AssetManager assetManager = Locators.getAssetManager();
        try {
            rootSpatial = assetManager.loadModel(assetPath);
            loadException = null;
        } catch (Exception exception) {
            rootSpatial = null;
            loadException = exception;
        }
        Locators.restore();

        if (rootSpatial != null) {
            ragdoll = removeDac();
            recalculateInitTransform();
            recalculateInfluence();

            int numBones = countBones();
            BitSet bitset = new BitSet(numBones); // empty set
            if (ragdoll != null) {
                for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
                    String name = boneName(boneIndex);
                    if (ragdoll.hasBoneLink(name)) {
                        bitset.set(boneIndex);
                    }
                }
            }
            setLinkedBones(bitset);
        }
    }

    /**
     * Read the exception that occurred during the most recent load attempt.
     *
     * @return the exception message, or "" if none
     */
    String loadExceptionString() {
        String result = "";
        if (loadException != null) {
            result = loadException.toString();
        }

        return result;
    }

    /**
     * Shift one component of the file-system path from the asset root to the
     * asset path.
     */
    void morePath() {
        unload();
        --numComponentsInRoot;
    }

    /**
     * Shift one component of the file-system path from the asset path to the
     * asset root.
     */
    void moreRoot() {
        unload();
        ++numComponentsInRoot;
    }

    /**
     * Determine the index of the parent of the indexed bone.
     *
     * @param boneIndex which bone (&ge;0)
     * @return the index of the parent (&ge;0) or -1 for a root bone
     */
    int parentIndex(int boneIndex) {
        if (rootSpatial == null) {
            throw new RuntimeException("No model loaded.");
        }

        int result;
        Skeleton skeleton = findSkeleton();
        if (skeleton == null) {
            Armature armature = findArmature();
            Joint joint = armature.getJoint(boneIndex);
            Joint parent = joint.getParent();

            if (parent == null) {
                result = -1;
            } else {
                result = armature.getJointIndex(parent);
            }

        } else {
            Bone bone = skeleton.getBone(boneIndex);
            Bone parent = bone.getParent();

            if (parent == null) {
                result = -1;
            } else {
                result = skeleton.getBoneIndex(parent);
            }
        }

        return result;
    }

    /**
     * If the range-of-motion task is done, instantiate the DynamicAnimControl
     * and proceed to LinksScreen.
     */
    void pollForTaskCompletion() {
        if (romTask == null || !romTask.isDone()) {
            return;
        }
        logger.log(Level.INFO, "The range-of-motion task is done.");

        RangeOfMotion[] roms;
        try {
            roms = romTask.get();
        } catch (ExecutionException | InterruptedException exception) {
            System.out.print(exception);
            return;
        }
        romCallable.cleanup();
        romTask = null;

        ragdoll = new DynamicAnimControl();
        float massParameter = 1f;
        LinkConfig linkConfig = new LinkConfig(massParameter,
                MassHeuristic.Density, ShapeHeuristic.VertexHull,
                new Vector3f(1f, 1f, 1f), CenterHeuristic.Mean);

        ragdoll.setConfig(DacConfiguration.torsoName, linkConfig);

        int numBones = countBones();
        for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
            if (linkedBones.get(boneIndex)) {
                String boneName = boneName(boneIndex);
                ragdoll.link(boneName, linkConfig, roms[boneIndex]);
            }
        }

        selectLink(DacConfiguration.torsoName);
        InputMode links = InputMode.findMode("links");
        links.setEnabled(true);
    }

    /**
     * Read the joint limits of the named BoneLink.
     *
     * @param boneName the name of the bone (not null, not empty)
     * @return the pre-existing limits (not null)
     */
    RangeOfMotion rom(String boneName) {
        assert boneName != null;
        assert !boneName.isEmpty();

        RangeOfMotion result = ragdoll.getJointLimits(boneName);
        return result;
    }

    /**
     * Determine which physics link is selected.
     *
     * @return the bone/torso name of the link, or null if no selection
     */
    String selectedLink() {
        return selectedLink;
    }

    /**
     * Select the named physics link.
     *
     * @param boneName the bone/torso name of the desired link (not null)
     */
    void selectLink(String boneName) {
        assert boneName != null;
        selectedLink = boneName;
    }

    /**
     * Replace the configuration of the named bone/torso.
     *
     * @param boneName the name of the bone, or torsoName (not null)
     * @param config the desired LinkConfig (not null)
     */
    void setConfig(String boneName, LinkConfig config) {
        assert boneName != null;
        assert config != null;

        ragdoll.setConfig(boneName, config);
    }

    /**
     * Alter the model's file-system path.
     *
     * @param path the desired file-system path (not null, contains a "/")
     */
    void setFilePath(String path) {
        assert path != null;
        assert path.contains("/");

        filePathComponents = path.split("/");
        numComponentsInRoot = 1;
        loadException = null;
        unload();
        /*
         * Use heuristics to guess how many components there are
         * in the file-system path to the asset root.
         */
        int numComponents = filePathComponents.length;
        assert numComponents > 0 : numComponents;
        for (int componentI = 0; componentI < numComponents; ++componentI) {
            String component = filePathComponents[componentI];
            switch (component) {
                case "assets":
                case "resources":
                case "Written Assets":
                    if (componentI > 1) {
                        numComponentsInRoot = componentI - 1;
                    }
                    break;
                case "Models":
                    if (componentI > 0 && componentI < numComponents) {
                        numComponentsInRoot = componentI;
                    }
                    break;
            }
        }
    }

    /**
     * Alter which bones will be linked. A C-G model must be loaded.
     *
     * @param linkedBones the desired set of linked bones
     */
    void setLinkedBones(BitSet linkedBones) {
        assert linkedBones != null;
        if (rootSpatial == null) {
            throw new RuntimeException("No model loaded.");
        }

        if (!linkedBones.equals(this.linkedBones)) {
            this.linkedBones = linkedBones;
            int numBones = countBones();
            String[] managerMap = new String[numBones];

            Skeleton skeleton = findSkeleton();
            if (skeleton == null) {
                Armature armature = findArmature();
                for (int jointIndex = 0; jointIndex < numBones; ++jointIndex) {
                    Joint joint = armature.getJoint(jointIndex);
                    managerMap[jointIndex] = findManager(joint, armature);
                }

            } else {
                for (int boneIndex = 0; boneIndex < numBones; ++boneIndex) {
                    Bone bone = skeleton.getBone(boneIndex);
                    managerMap[boneIndex] = findManager(bone, skeleton);
                }
            }

            List<Mesh> targetList = RagUtils.listDacMeshes(rootSpatial, null);
            Mesh[] targets = new Mesh[targetList.size()];
            targetList.toArray(targets);
            /*
             * Enumerate mesh-vertex coordinates and assign them to managers.
             */
            coordsMap = RagUtils.coordsMap(targets, managerMap);
        }
    }

    /**
     * Replace the RangeOfMotion of the named BoneLink.
     *
     * @param boneName the name of the bone (not null, not empty)
     * @param rom the desired RangeOfMotion (not null)
     */
    void setRom(String boneName, RangeOfMotion rom) {
        assert boneName != null;
        assert !boneName.isEmpty();
        assert rom != null;

        ragdoll.setJointLimits(boneName, rom);
    }

    /**
     * Start a thread to estimate the range of motion for each linked bone.
     */
    void startRomTask() {
        romCallable = new RomCallable(this);
        assert romTask == null;
        romTask = new FutureTask<>(romCallable);
        Thread romThread = new Thread(romTask);
        romThread.start();
    }

    /**
     * Toggle the visibility of PhysicsJoint axes in TestScreen.
     */
    void toggleShowingAxes() {
        isShowingAxes = !isShowingAxes;
    }

    /**
     * Unload the loaded C-G model, if any.
     */
    void unload() {
        rootSpatial = null;
        ragdoll = null;
    }

    /**
     * Validate the loaded C-G model for use with DynamicAnimControl.
     *
     * @return a feedback message (not null, not empty) or "" if none
     */
    String validationFeedback() {
        if (rootSpatial == null) {
            throw new RuntimeException("No model loaded.");
        }

        String result = "";
        Skeleton skeleton = findSkeleton();
        if (skeleton == null) {
            Armature armature = findArmature();
            try {
                RagUtils.validate(rootSpatial);
                RagUtils.validate(armature);
            } catch (IllegalArgumentException exception) {
                result = exception.getMessage();
            }

        } else {
            try {
                RagUtils.validate(rootSpatial);
                RagUtils.validate(skeleton);
            } catch (IllegalArgumentException exception) {
                result = exception.getMessage();
            }
        }

        return result;
    }
    // *************************************************************************
    // private methods

    /**
     * Access the model's Armature, assuming it doesn't have more than one
     * SkinningControl. A C-G model must be loaded.
     *
     * @return the pre-existing instance, or null if none or multiple
     */
    private Armature findArmature() {
        if (rootSpatial == null) {
            throw new RuntimeException("No model loaded.");
        }

        List<SkinningControl> skinners = MySpatial.listControls(rootSpatial,
                SkinningControl.class, null);

        Armature result = null;
        if (skinners.size() == 1) {
            SkinningControl control = skinners.get(0);
            result = control.getArmature();
        }

        return result;
    }

    /**
     * Find the manager of the specified Bone.
     *
     * @param startBone which Bone to analyze (not null, unaffected)
     * @param skeleton the Skeleton containing the Bone
     * @return a bone/torso name (not null)
     */
    private String findManager(Bone startBone, Skeleton skeleton) {
        assert startBone != null;

        String managerName;
        Bone bone = startBone;
        while (true) {
            int boneIndex = skeleton.getBoneIndex(bone);
            if (linkedBones.get(boneIndex)) {
                managerName = bone.getName();
                break;
            }
            bone = bone.getParent();
            if (bone == null) {
                managerName = DacConfiguration.torsoName;
                break;
            }
        }

        assert managerName != null;
        return managerName;
    }

    /**
     * Find the manager of the specified Joint.
     *
     * @param startJoint which Joint to analyze (not null, unaffected)
     * @param skeleton the Armature containing the Joint
     * @return a bone/torso name (not null)
     */
    private String findManager(Joint startJoint, Armature skeleton) {
        assert startJoint != null;

        String managerName;
        Joint joint = startJoint;
        while (true) {
            int jointIndex = skeleton.getJointIndex(joint);
            if (linkedBones.get(jointIndex)) {
                managerName = joint.getName();
                break;
            }
            joint = joint.getParent();
            if (joint == null) {
                managerName = DacConfiguration.torsoName;
                break;
            }
        }

        assert managerName != null;
        return managerName;
    }

    /**
     * Access the model's Skeleton, assuming it doesn't have more than one
     * SkeletonControl. A C-G model must be loaded.
     *
     * @return the pre-existing instance, or null if none or multiple
     */
    private Skeleton findSkeleton() {
        if (rootSpatial == null) {
            throw new RuntimeException("No model loaded.");
        }

        SkeletonControl control = RagUtils.findSkeletonControl(rootSpatial);
        Skeleton result = null;
        if (control != null) {
            result = control.getSkeleton();
        }

        return result;
    }

    /**
     * Recalculate the influence of each bone.
     */
    private void recalculateInfluence() {
        Skeleton skeleton = findSkeleton();
        if (skeleton == null) {
            Armature armature = findArmature();
            if (armature != null) {
                anyInfluenceBones = InfluenceUtil.addAllInfluencers(
                        rootSpatial, armature);
                armature.applyBindPose();
            }
        } else {
            anyInfluenceBones = InfluenceUtil.addAllInfluencers(rootSpatial,
                    skeleton);
        }

        int numBones = countBones();
        directInfluenceBones = new BitSet(numBones);
        InfluenceUtil.addDirectInfluencers(rootSpatial, directInfluenceBones);
    }

    /**
     * Recalculate the initial Transform for visualization.
     */
    private void recalculateInitTransform() {
        Spatial cgmCopy = (Spatial) Heart.deepCopy(rootSpatial);
        /**
         * Scale the copy uniformly to the desired height, assuming Y-up
         * orientation.
         */
        Vector3f[] minMax = MySpatial.findMinMaxCoords(cgmCopy);
        Vector3f min = minMax[0];
        Vector3f max = minMax[1];
        float oldHeight = max.y - min.y;
        if (oldHeight > 0f) {
            cgmCopy.scale(cgmHeight / oldHeight);
        }
        /**
         * Translate a copy's center so that it rests on the X-Z plane, and its
         * center lies on the Y axis.
         */
        minMax = MySpatial.findMinMaxCoords(cgmCopy);
        min = minMax[0];
        max = minMax[1];
        Vector3f center = MyVector3f.midpoint(min, max, null);
        Vector3f offset = new Vector3f(center.x, min.y, center.z);
        Vector3f location = cgmCopy.getWorldTranslation();
        location.subtractLocal(offset);
        MySpatial.setWorldLocation(cgmCopy, location);

        initTransform.set(cgmCopy.getLocalTransform());
    }

    /**
     * Remove any DynamicAnimControl from the C-G model.
     *
     * @return the pre-existing control that was removed, or null if none
     */
    private DynamicAnimControl removeDac() {
        List<DynamicAnimControl> list = MySpatial.listControls(rootSpatial,
                DynamicAnimControl.class, null);
        DynamicAnimControl result = null;
        if (!list.isEmpty()) {
            assert list.size() == 1 : list.size();
            result = list.get(0);
            Spatial controlled = result.getSpatial();
            controlled.removeControl(result);
        }

        return result;
    }
}
