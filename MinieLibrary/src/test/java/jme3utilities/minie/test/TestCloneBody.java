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

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.bullet.SoftBodyWorldInfo;
import com.jme3.bullet.collision.AfMode;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.control.SoftBodyControl;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.bullet.objects.infos.ConfigFlag;
import com.jme3.bullet.objects.infos.Sbcp;
import com.jme3.bullet.objects.infos.SoftBodyConfig;
import com.jme3.bullet.objects.infos.SoftBodyMaterial;
import com.jme3.bullet.util.NativeSoftBodyUtil;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.debug.WireBox;
import com.jme3.system.NativeLibraryLoader;
import jme3utilities.Heart;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test cloning/saving/loading on PhysicsBody and all its subclasses.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestCloneBody {
    // *************************************************************************
    // fields

    /**
     * AssetManager required by the BinaryImporter
     */
    final private AssetManager assetManager = new DesktopAssetManager();
    /**
     * collision shape for generating rigid bodies
     */
    private CollisionShape shape;
    /**
     * wire mesh for generating soft bodies
     */
    final private Mesh wireBox = new WireBox();
    // *************************************************************************
    // new methods exposed

    /**
     * Test cloning/saving/loading on subclasses of PhysicsBody. TODO add to
     * PhysicsSpace, add joints
     */
    @Test
    public void testCloneBody() {
        NativeLibraryLoader.loadNativeLibrary("bulletjme", true);
        shape = new SphereCollisionShape(1f);

        for (int iteration = 0; iteration < 99; ++iteration) {
            clonePrb();
            cloneRbc();
            clonePv();
            cloneVc();
            clonePsb();
            cloneSbc();
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Clone rigid bodies.
     */
    private void clonePrb() {
        /*
         * static
         */
        PhysicsRigidBody body0 = new PhysicsRigidBody(shape, 0f);
        setParameters(body0, 0f);
        verifyParameters(body0, 0f);
        PhysicsRigidBody body0Clone = Heart.deepCopy(body0);
        cloneTest(body0, body0Clone);
        /*
         * dynamic with mass=1
         */
        PhysicsRigidBody body = new PhysicsRigidBody(shape, 1f);
        setParameters(body, 0f);
        verifyParameters(body, 0f);
        PhysicsRigidBody bodyClone = Heart.deepCopy(body);
        cloneTest(body, bodyClone);
    }

    /**
     * Clone soft bodies.
     */
    private void clonePsb() {
        /*
         * empty
         */
        PhysicsSoftBody soft = new PhysicsSoftBody();
        setParameters(soft, 0f);
        verifyParameters(soft, 0f);
        PhysicsSoftBody softClone = Heart.deepCopy(soft);
        cloneTest(soft, softClone);
        /*
         * non-empty
         */
        PhysicsSoftBody soft2 = new PhysicsSoftBody();
        NativeSoftBodyUtil.appendFromLineMesh(wireBox, soft2);
        setParameters(soft2, 0f);
        verifyParameters(soft2, 0f);
        PhysicsSoftBody soft2Clone = Heart.deepCopy(soft2);
        cloneTest(soft2, soft2Clone);
    }

    /**
     * Clone vehicles.
     */
    private void clonePv() {
        /*
         * TODO add wheel(s)
         */
        PhysicsVehicle vehicle = new PhysicsVehicle(shape, 1f);
        setParameters(vehicle, 0f);
        verifyParameters(vehicle, 0f);
        PhysicsVehicle vehicleClone = Heart.deepCopy(vehicle);
        cloneTest(vehicle, vehicleClone);
    }

    /**
     * Clone rigid-body controls.
     */
    private void cloneRbc() {
        RigidBodyControl rbc = new RigidBodyControl(shape, 1f);
        setParameters(rbc, 0f);
        verifyParameters(rbc, 0f);
        RigidBodyControl rbcClone = Heart.deepCopy(rbc);
        cloneTest(rbc, rbcClone);
    }

    /**
     * Clone soft-body controls.
     */
    private void cloneSbc() {
        boolean localPhysics = false;
        boolean updateNormals = true;
        boolean mergeVertices = true;
        SoftBodyControl sbc = new SoftBodyControl(localPhysics, updateNormals,
                mergeVertices);
        Geometry sbcGeom = new Geometry("sbcGeom", wireBox);
        sbcGeom.addControl(sbc);
        PhysicsSoftBody soft3 = sbc.getBody();
        setParameters(soft3, 0f);
        verifyParameters(soft3, 0f);
        Geometry sbcGeomClone = Heart.deepCopy(sbcGeom);
        SoftBodyControl sbcClone = (SoftBodyControl) sbcGeomClone.getControl(0);
        PhysicsSoftBody soft3Clone = sbcClone.getBody();
        cloneTest(soft3, soft3Clone);
    }

    private void cloneTest(PhysicsBody body, PhysicsBody bodyClone) {
        assert bodyClone.nativeId() != body.nativeId();
        if (body instanceof PhysicsSoftBody) {
            PhysicsSoftBody sBody = (PhysicsSoftBody) body;
            PhysicsSoftBody sBodyClone = (PhysicsSoftBody) bodyClone;
            assert sBodyClone.getSoftConfig() != sBody.getSoftConfig();
            //assert sBodyClone.getSoftMaterial() != sBody.getSoftMaterial();
            assert sBodyClone.getWorldInfo() != sBody.getWorldInfo();
        }

        verifyParameters(body, 0f);
        verifyParameters(bodyClone, 0f);

        setParameters(body, 0.3f);
        verifyParameters(body, 0.3f);
        verifyParameters(bodyClone, 0f);

        setParameters(bodyClone, 0.6f);
        verifyParameters(body, 0.3f);
        verifyParameters(bodyClone, 0.6f);

        PhysicsBody bodyCopy = BinaryExporter.saveAndLoad(assetManager, body);
        verifyParameters(bodyCopy, 0.3f);

        PhysicsBody bodyCloneCopy
                = BinaryExporter.saveAndLoad(assetManager, bodyClone);
        verifyParameters(bodyCloneCopy, 0.6f);

        PhysicsBody xmlCopy = MinieTest.saveAndLoadXml(assetManager, body);
        verifyParameters(xmlCopy, 0.3f);
    }

    /**
     * Clone vehicle controls.
     */
    private void cloneVc() {
        /*
         * TODO add wheel(s)
         */
        VehicleControl vc = new VehicleControl(shape, 1f);
        setParameters(vc, 0f);
        verifyParameters(vc, 0f);
        VehicleControl vcClone = Heart.deepCopy(vc);
        cloneTest(vc, vcClone);
    }

    private void setParameters(PhysicsBody pco, float b) {
        if (pco instanceof PhysicsRigidBody) {
            setRigid((PhysicsRigidBody) pco, b);
        } else if (pco instanceof PhysicsSoftBody) {
            setSoft((PhysicsSoftBody) pco, b);
        } else {
            throw new IllegalArgumentException(pco.getClass().getName());
        }
    }

    private void setRigid(PhysicsRigidBody body, float b) {
        boolean flag = (b > 0.15f && b < 0.45f);
        body.setContactResponse(flag);
        if (body.getMass() != PhysicsBody.massForStatic) {
            body.setKinematic(!flag);
        }
        body.setProtectGravity(!flag);

        int afMode = Math.round(b / 0.3f);
        body.setAnisotropicFriction(
                new Vector3f(b + 0.004f, b + 0.005f, b + 0.006f), afMode);

        body.setAngularDamping(b + 0.01f);
        Vector3f aFactor = new Vector3f(b + 0.02f, b + 0.021f, b + 0.022f);
        body.setAngularFactor(aFactor);
        body.setSleepingThresholds(b + 0.17f, b + 0.03f);
        body.setAngularVelocity(new Vector3f(b + 0.04f, b + 0.05f, b + 0.06f));
        body.setCcdMotionThreshold(b + 0.07f);
        body.setCcdSweptSphereRadius(b + 0.08f);
        body.setContactDamping(b + 0.084f);
        body.setContactProcessingThreshold(b + 0.0845f);
        body.setContactStiffness(b + 0.085f);
        body.setFriction(b + 0.09f);
        body.setInverseInertiaLocal(
                new Vector3f(b + 0.122f, b + 0.123f, b + 0.124f));
        body.setLinearDamping(b + 0.13f);
        Vector3f lFactor = new Vector3f(b + 0.14f, b + 0.15f, b + 0.16f);
        body.setLinearFactor(lFactor);
        body.setPhysicsLocation(new Vector3f(b + 0.18f, b + 0.19f, b + 0.20f));

        Quaternion orient
                = new Quaternion(b + 0.21f, b + 0.22f, b + 0.23f, b + 0.24f);
        orient.normalizeLocal();
        Matrix3f matrix = orient.toRotationMatrix();
        body.setPhysicsRotation(matrix);

        body.setRestitution(b + 0.25f);
        body.setRollingFriction(b + 0.254f);
        body.setSpinningFriction(b + 0.255f);
        /*
         * Linear velocity affects deactivation time, so set it first!
         */
        body.clearForces();
        Vector3f force = new Vector3f(b + 0.231f, b + 0.232f, b + 0.233f);
        body.applyCentralForce(force.divide(lFactor));
        Vector3f torque = new Vector3f(b + 0.241f, b + 0.242f, b + 0.243f);
        body.applyTorque(torque.divide(aFactor));
        body.setLinearVelocity(new Vector3f(b + 0.26f, b + 0.27f, b + 0.28f));
        body.setDeactivationTime(b + 0.087f);
        if (body instanceof PhysicsVehicle) {
            // TODO
        }
    }

    private void setSoft(PhysicsSoftBody body, float b) {
        boolean flag = (b > 0.15f && b < 0.45f);
        int n = Math.round(10f * b);

        body.setProtectWorldInfo(!flag);

        SoftBodyConfig config = body.getSoftConfig();
        for (Sbcp sbcp : Sbcp.values()) {
            float value = b + 0.001f * sbcp.ordinal();
            config.set(sbcp, value);
        }

        SoftBodyWorldInfo info = body.getWorldInfo();
        info.setAirDensity(b + 0.03f);
        info.setGravity(new Vector3f(b + 0.031f, b + 0.032f, b + 0.033f));
        info.setMaxDisplacement(b + 0.034f);
        info.setWaterDensity(b + 0.035f);
        info.setWaterOffset(b + 0.036f);

        Vector3f normal = new Vector3f(b + 0.1f, b + 0.2f, b + 0.3f);
        normal.normalizeLocal();
        info.setWaterNormal(normal);

        config.setClusterIterations(n);
        config.setDriftIterations(n + 1);
        config.setPositionIterations(n + 2);
        config.setVelocityIterations(n + 3);

        int flags;
        if (flag) {
            flags = ConfigFlag.CL_RS | ConfigFlag.CL_SS | ConfigFlag.CL_SELF;
        } else {
            flags = ConfigFlag.SDF_RS | ConfigFlag.VF_SS;
        }
        config.setCollisionFlags(flags);

        SoftBodyMaterial material = body.getSoftMaterial();
        material.setAngularStiffness(b + 0.04f);
        material.setLinearStiffness(b + 0.041f);
        material.setVolumeStiffness(b + 0.042f);
    }

    private void verifyParameters(PhysicsBody pco, float b) {
        Assert.assertNotNull(pco);
        if (pco instanceof PhysicsRigidBody) {
            verifyRigid((PhysicsRigidBody) pco, b);
        } else if (pco instanceof PhysicsSoftBody) {
            verifySoft((PhysicsSoftBody) pco, b);
        } else {
            throw new IllegalArgumentException(pco.getClass().getName());
        }
    }

    private void verifyRigid(PhysicsRigidBody body, float b) {
        boolean flag = (b > 0.15f && b < 0.45f);
        assert body.isContactResponse() == flag;
        if (body.getMass() != PhysicsBody.massForStatic) {
            assert body.isKinematic() == !flag;
        }
        assert body.isGravityProtected() == !flag;

        int index = Math.round(b / 0.3f);
        if (index == 0) {
            assert !body.hasAnisotropicFriction(AfMode.either);
        } else {
            assert body.hasAnisotropicFriction(index);
            Vector3f c = body.getAnisotropicFriction(null);
            assert c.x == b + 0.004f : c;
            assert c.y == b + 0.005f : c;
            assert c.z == b + 0.006f : c;
        }

        assert body.getAngularDamping() == b + 0.01f;

        Vector3f af = body.getAngularFactor(null);
        assert af.x == b + 0.02f : af;
        assert af.y == b + 0.021f : af;
        assert af.z == b + 0.022f : af;

        assert body.getAngularSleepingThreshold() == b + 0.03f;

        assert body.getCcdMotionThreshold() == b + 0.07f;
        assert body.getCcdSweptSphereRadius() == b + 0.08f;
        assert body.getContactDamping() == b + 0.084f;
        assert body.getContactProcessingThreshold() == b + 0.0845f;
        assert body.getContactStiffness() == b + 0.085f;
        Assert.assertEquals(b + 0.087f, body.getDeactivationTime(), 0f);
        assert body.getFriction() == b + 0.09f;

        Vector3f i = body.getInverseInertiaLocal(null);
        assert i.x == b + 0.122f : i;
        assert i.y == b + 0.123f : i;
        assert i.z == b + 0.124f : i;

        assert body.getLinearDamping() == b + 0.13f;

        Vector3f f = body.getLinearFactor(null);
        assert f.x == b + 0.14f : f;
        assert f.y == b + 0.15f : f;
        assert f.z == b + 0.16f : f;

        assert body.getLinearSleepingThreshold() == b + 0.17f;

        Vector3f x = body.getPhysicsLocation(null);
        assert x.x == b + 0.18f : x;
        assert x.y == b + 0.19f : x;
        assert x.z == b + 0.20f : x;

        Quaternion orient
                = new Quaternion(b + 0.21f, b + 0.22f, b + 0.23f, b + 0.24f);
        orient.normalizeLocal();
        Matrix3f matrix = orient.toRotationMatrix();
        Matrix3f m = body.getPhysicsRotationMatrix(null);
        assert m.equals(matrix);

        assert body.getRestitution() == b + 0.25f;
        assert body.getRollingFriction() == b + 0.254f;
        assert body.getSpinningFriction() == b + 0.255f;

        MinieTest.assertEquals(b + 0.231f, b + 0.232f, b + 0.233f,
                body.totalAppliedForce(null), 1e-6f);
        MinieTest.assertEquals(b + 0.241f, b + 0.242f, b + 0.243f,
                body.totalAppliedTorque(null), 1e-6f);

        if (body.isDynamic()) {
            Vector3f w = body.getAngularVelocity(null);
            assert w.x == b + 0.04f : w;
            assert w.y == b + 0.05f : w;
            assert w.z == b + 0.06f : w;

            Vector3f v = body.getLinearVelocity(null);
            assert v.x == b + 0.26f : v;
            assert v.y == b + 0.27f : v;
            assert v.z == b + 0.28f : v;
        }

        if (body instanceof PhysicsVehicle) {
            // TODO
        }
    }

    private void verifySoft(PhysicsSoftBody body, float b) {
        boolean flag = (b > 0.15f && b < 0.45f);
        int n = Math.round(10f * b);

        assert body.isWorldInfoProtected() == !flag;

        SoftBodyConfig config = body.getSoftConfig();
        for (Sbcp sbcp : Sbcp.values()) {
            float expected = b + 0.001f * sbcp.ordinal();
            float actual = config.get(sbcp);
            Assert.assertEquals(expected, actual, 1e-6f);
        }

        SoftBodyWorldInfo info = body.getWorldInfo();
        Assert.assertEquals(b + 0.03f, info.airDensity(), 0f);
        MinieTest.assertEquals(b + 0.031f, b + 0.032f, b + 0.033f,
                info.copyGravity(null), 0f);
        Assert.assertEquals(b + 0.034f, info.maxDisplacement(), 0f);
        Assert.assertEquals(b + 0.035f, info.waterDensity(), 0f);
        Assert.assertEquals(b + 0.036f, info.waterOffset(), 0f);

        Vector3f normal = new Vector3f(b + 0.1f, b + 0.2f, b + 0.3f);
        normal.normalizeLocal();
        MinieTest.assertEquals(normal.x, normal.y, normal.z,
                info.copyWaterNormal(null), 1e-5f);

        assert config.clusterIterations() == n;
        assert config.driftIterations() == n + 1;
        assert config.positionIterations() == n + 2;
        assert config.velocityIterations() == n + 3;

        int flags;
        if (flag) {
            flags = ConfigFlag.CL_RS | ConfigFlag.CL_SS | ConfigFlag.CL_SELF;
        } else {
            flags = ConfigFlag.SDF_RS | ConfigFlag.VF_SS;
        }
        assert config.collisionFlags() == flags;

        SoftBodyMaterial material = body.getSoftMaterial();
        Assert.assertEquals(b + 0.04f, material.angularStiffness(), 0f);
        Assert.assertEquals(b + 0.041f, material.linearStiffness(), 0f);
        Assert.assertEquals(b + 0.042f, material.volumeStiffness(), 0f);
    }
}
