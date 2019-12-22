/*
 * Copyright (c) 2009-2018 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.bullet.collision.shapes;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import com.jme3.util.clone.Cloner;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.logging.Logger;
import jme3utilities.Validate;
import jme3utilities.math.MyBuffer;
import jme3utilities.math.MyVector3f;
import jme3utilities.math.MyVolume;

/**
 * A cylindrical CollisionShape based on Bullet's btCylinderShapeX,
 * btCylinderShape, or btCylinderShapeZ.
 *
 * @author normenhansen
 */
public class CylinderCollisionShape extends CollisionShape {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger2
            = Logger.getLogger(CylinderCollisionShape.class.getName());
    /**
     * field names for serialization
     */
    final private static String tagAxis = "axis";
    final private static String tagHalfExtents = "halfExtents";
    // *************************************************************************
    // fields

    /**
     * copy of main (height) axis (0&rarr;X, 1&rarr;Y, 2&rarr;Z)
     */
    private int axis;
    /**
     * copy of unscaled half extent for each local axis (not null, no negative
     * component)
     */
    private Vector3f halfExtents = new Vector3f(0.5f, 0.5f, 0.5f);
    // *************************************************************************
    // constructors

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    public CylinderCollisionShape() {
    }

    /**
     * Instantiate a cylinder shape that encloses the sample locations in the
     * specified FloatBuffer range.
     *
     * @param buffer the buffer that contains the sample locations (not null,
     * unaffected)
     * @param startPosition the position at which the sample locations start
     * (&ge;0, &le;endPosition)
     * @param endPosition the position at which the sample locations end
     * (&ge;startPosition, &le;capacity)
     * @param axisIndex local axis for height: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     */
    public CylinderCollisionShape(FloatBuffer buffer, int startPosition,
            int endPosition, int axisIndex) {
        Validate.nonNull(buffer, "buffer");
        Validate.inRange(startPosition, "start position", 0, endPosition);
        Validate.inRange(endPosition, "end position", startPosition,
                buffer.capacity());
        Validate.inRange(axisIndex, "axis index", PhysicsSpace.AXIS_X,
                PhysicsSpace.AXIS_Z);

        MyBuffer.maxAbs(buffer, startPosition, endPosition, halfExtents);

        float radius = MyBuffer.cylinderRadius(buffer, startPosition,
                endPosition, axisIndex);
        switch (axisIndex) {
            case PhysicsSpace.AXIS_X:
                halfExtents.y = radius;
                halfExtents.z = radius;
                break;
            case PhysicsSpace.AXIS_Y:
                halfExtents.x = radius;
                halfExtents.z = radius;
                break;
            case PhysicsSpace.AXIS_Z:
                halfExtents.x = radius;
                halfExtents.y = radius;
                break;
            default:
                String message = Integer.toString(axisIndex);
                throw new RuntimeException(message);
        }
        assert MyVector3f.isAllNonNegative(halfExtents) : halfExtents;

        createShape();
    }

    /**
     * Instantiate a Z-axis cylinder shape with the specified half extents.
     *
     * @param halfExtents the desired unscaled half extents (not null, no
     * negative component, unaffected)
     */
    public CylinderCollisionShape(Vector3f halfExtents) {
        Validate.nonNegative(halfExtents, "half extents");

        this.halfExtents.set(halfExtents);
        axis = PhysicsSpace.AXIS_Z;
        createShape();
    }

    /**
     * Instantiate a cylinder shape around the specified axis.
     *
     * @param halfExtents the desired unscaled half extents (not null, no
     * negative component, unaffected)
     * @param axisIndex local axis for height: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     */
    public CylinderCollisionShape(Vector3f halfExtents, int axisIndex) {
        Validate.nonNegative(halfExtents, "half extents");
        Validate.inRange(axisIndex, "axis index", PhysicsSpace.AXIS_X,
                PhysicsSpace.AXIS_Z);

        this.halfExtents.set(halfExtents);
        this.axis = axisIndex;
        createShape();
    }
    // *************************************************************************
    // new methods exposed TODO getHeight()

    /**
     * Determine the main (height) axis of the cylinder.
     *
     * @return the axis index: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     */
    public int getAxis() {
        assert axis == PhysicsSpace.AXIS_X
                || axis == PhysicsSpace.AXIS_Y
                || axis == PhysicsSpace.AXIS_Z : axis;
        return axis;
    }

    /**
     * Copy the half extents of the cylinder.
     *
     * @param storeResult storage for the result (modified if not null)
     * @return the unscaled half extent for each local axis (either storeResult
     * or a new vector, not null, no negative component)
     */
    public Vector3f getHalfExtents(Vector3f storeResult) {
        assert MyVector3f.isAllNonNegative(halfExtents) : halfExtents;
        if (storeResult == null) {
            return halfExtents.clone();
        } else {
            return storeResult.set(halfExtents);
        }
    }

    /**
     * Calculate the unscaled volume of the cylinder.
     *
     * @return the volume (in shape-space units cubed, &ge;0)
     */
    public float unscaledVolume() {
        float result = MyVolume.cylinderVolume(halfExtents);

        assert result >= 0f : result;
        return result;
    }
    // *************************************************************************
    // CollisionShape methods

    /**
     * Test whether the specified scaling factors can be applied to this shape.
     * For cylinder shapes, radial scaling must be uniform.
     *
     * @param scale the desired scaling factor for each local axis (may be null,
     * unaffected)
     * @return true if applicable, otherwise false
     */
    @Override
    public boolean canScale(Vector3f scale) {
        boolean canScale = super.canScale(scale);
        if (canScale) {
            if (axis == PhysicsSpace.AXIS_X && scale.y != scale.z) {
                canScale = false;
            } else if (axis == PhysicsSpace.AXIS_Y && scale.x != scale.z) {
                canScale = false;
            } else if (axis == PhysicsSpace.AXIS_Z && scale.x != scale.y) {
                canScale = false;
            }
        }

        return canScale;
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned shape into a deep-cloned one, using the specified Cloner
     * and original to resolve copied fields.
     *
     * @param cloner the Cloner that's cloning this shape (not null)
     * @param original the instance from which this shape was shallow-cloned
     * (not null, unaffected)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);
        halfExtents = cloner.clone(halfExtents);
        createShape();
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new instance
     */
    @Override
    public CylinderCollisionShape jmeClone() {
        try {
            CylinderCollisionShape clone
                    = (CylinderCollisionShape) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * De-serialize this shape from the specified importer, for example when
     * loading from a J3O file.
     *
     * @param importer (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        super.read(importer);
        InputCapsule capsule = importer.getCapsule(this);

        Vector3f he = (Vector3f) capsule.readSavable(tagHalfExtents,
                new Vector3f(0.5f, 0.5f, 0.5f));
        halfExtents.set(he);
        axis = capsule.readInt(tagAxis, PhysicsSpace.AXIS_Y);
        createShape();
    }

    /**
     * Serialize this shape to the specified exporter, for example when saving
     * to a J3O file.
     *
     * @param exporter (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter exporter) throws IOException {
        super.write(exporter);
        OutputCapsule capsule = exporter.getCapsule(this);

        capsule.write(halfExtents, tagHalfExtents, null);
        capsule.write(axis, tagAxis, PhysicsSpace.AXIS_Y);
    }
    // *************************************************************************
    // private methods

    /**
     * Instantiate the configured shape in Bullet.
     */
    private void createShape() {
        assert axis == PhysicsSpace.AXIS_X
                || axis == PhysicsSpace.AXIS_Y
                || axis == PhysicsSpace.AXIS_Z : axis;
        assert MyVector3f.isAllNonNegative(halfExtents) : halfExtents;

        long shapeId = createShape(axis, halfExtents);
        setNativeId(shapeId);

        setScale(scale);
        setMargin(margin);
    }
    // *************************************************************************
    // native methods

    native private long createShape(int axis, Vector3f halfExtents);
}
