/*
 Copyright (c) 2018-2019, Stephen Gold
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

import com.jme3.bullet.animation.IKController;
import com.jme3.bullet.animation.PhysicsLink;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;

/**
 * A IK controller to keep the controlled link upright. If the model oscillates,
 * try reducing the gain factors.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class UprightController extends IKController {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(UprightController.class.getName());
    /**
     * local copy of {@link com.jme3.math.Vector3f#UNIT_Y}
     */
    final private static Vector3f unitY = new Vector3f(0f, 1f, 0f);
    // *************************************************************************
    // fields

    /**
     * factor used to calculate the delta gain
     */
    private float deltaGainFactor = 0.1f;
    /**
     * factor used to calculate the error gain
     */
    private float errorGainFactor = 0.1f;
    /**
     * reusable matrix for calculating rotational inertia
     */
    private Matrix3f tmpInertia = new Matrix3f();
    /**
     * desired up direction (unit vector in the link body's local coordinates)
     */
    private Vector3f directionInLinkBody;
    /**
     * error from the previous timestep
     */
    private Vector3f previousError = new Vector3f();
    // *************************************************************************
    // constructors

    /**
     * Instantiate an enabled controller.
     *
     * @param controlledLink the link to be controlled (not null)
     * @param directionInLinkBody the up direction (in the link body's local
     * coordinates, not null, not zero, unaffected)
     */
    public UprightController(PhysicsLink controlledLink,
            Vector3f directionInLinkBody) {
        super(controlledLink);
        Validate.nonZero(directionInLinkBody, "direction in link body");
        this.directionInLinkBody = directionInLinkBody.normalize();
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Read the delta gain factor.
     *
     * @return the gain factor
     */
    public float deltaGainFactor() {
        return deltaGainFactor;
    }

    /**
     * Read the error gain factor.
     *
     * @return the gain factor
     */
    public float errorGainFactor() {
        return errorGainFactor;
    }

    /**
     * Alter the delta gain factor.
     *
     * @param newFactor (default = 0.1)
     */
    public void setDeltaGainFactor(float newFactor) {
        deltaGainFactor = newFactor;
    }

    /**
     * Alter the error gain factor.
     *
     * @param newFactor (default = 0.1)
     */
    public void setErrorGainFactor(float newFactor) {
        errorGainFactor = newFactor;
    }
    // *************************************************************************
    // IKController methods

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned controller into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this controller (not null)
     * @param original the instance from which this controller was
     * shallow-cloned (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);

        directionInLinkBody = cloner.clone(directionInLinkBody);
        previousError = cloner.clone(previousError);
    }

    /**
     * Apply an impulse to the controlled rigid body to keep the controlled link
     * upright. Meant to be invoked by the controlled link before each physics
     * tick.
     *
     * @param timeStep the physics timestep (in seconds, &ge;0)
     */
    @Override
    public void preTick(float timeStep) {
        Validate.nonNegative(timeStep, "time step");
        if (!isEnabled()) {
            return;
        }

        PhysicsLink link = getLink();
        assert !link.isKinematic();
        /*
         * Convert the body's "up" direction to physics-space coordinates.
         */
        Transform localToWorld = link.physicsTransform(null);
        Vector3f actual = localToWorld.getRotation().mult(directionInLinkBody);
        /*
         * error = actual X desired
         */
        Vector3f error = actual.cross(unitY);
        /*
         * Return early if the error angle is 0.
         * Magnify the error if the angle exceeds 90 degrees.
         */
        float absSinErrorAngle = error.length();
        if (absSinErrorAngle == 0f) {
            if (error.y >= 0f) { // No error at all!
                previousError.set(error);
                return;
            }
            // Error angle is 180 degrees!
            Vector3f ortho = new Vector3f();
            MyVector3f.generateBasis(unitY, ortho, new Vector3f());
            error = actual.cross(ortho);
            absSinErrorAngle = error.length();
        }
        Vector3f errorAxis = error.divide(absSinErrorAngle);
        float errorMagnitude = (error.y >= 0f) ? absSinErrorAngle : 1f;
        errorAxis.mult(errorMagnitude, error);
        /*
         * Calculate delta: the change in the error vector.
         */
        Vector3f delta = error.subtract(previousError, null);
        previousError.set(error);
        /*
         * Calculate a torque impulse.
         */
        Vector3f sum = new Vector3f(0f, 0f, 0f);
        // delta term
        MyVector3f.accumulateScaled(sum, delta, deltaGainFactor);
        // proportional term
        MyVector3f.accumulateScaled(sum, error, errorGainFactor);
        // scale by rotational inertia
        PhysicsRigidBody rigidBody = link.getRigidBody();
        rigidBody.getInverseInertiaWorld(tmpInertia);
        tmpInertia.invertLocal();
        tmpInertia.mult(sum, sum);
        /*
         * Apply the torque impulse to the controlled link's rigid body.
         */
        rigidBody.applyTorqueImpulse(sum);
    }

    /**
     * De-serialize this controller, for example when loading from a J3O file.
     *
     * @param importer the importer (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule ic = importer.getCapsule(this);

        deltaGainFactor = ic.readFloat("deltaGainFactor", 0.1f);
        errorGainFactor = ic.readFloat("errorGainFactor", 0.1f);
        directionInLinkBody = (Vector3f) ic.readSavable("directionInLinkBody",
                new Vector3f(1f, 0f, 0f));
        previousError = (Vector3f) ic.readSavable("previousError",
                new Vector3f());
    }

    /**
     * Serialize this controller, for example when saving to a J3O file.
     *
     * @param exporter the exporter (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        OutputCapsule oc = exporter.getCapsule(this);

        oc.write(deltaGainFactor, "deltaGainFactor", 0.1f);
        oc.write(errorGainFactor, "errorGainFactor", 0.1f);
        oc.write(directionInLinkBody, "directionInLinkBody", null);
        oc.write(previousError, "previousError", null);
    }
}
