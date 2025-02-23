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
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsBody;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;

/**
 * A simple example of rigid-body deactivation.
 *
 * Builds upon HelloStaticBody.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloDeactivation
        extends SimpleApplication
        implements PhysicsTickListener {
    // *************************************************************************
    // fields

    private static PhysicsRigidBody dynamicCube;
    private static PhysicsRigidBody staticCube;
    private static PhysicsSpace physicsSpace;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloDeactivation application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        HelloDeactivation application = new HelloDeactivation();
        application.start();
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void simpleInitApp() {
        // Set up Bullet physics and create a physics space.
        BulletAppState bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        physicsSpace = bulletAppState.getPhysicsSpace();

        // To enable the callbacks, add this application as a tick listener.
        physicsSpace.addTickListener(this);

        // Enable debug visualization to reveal what occurs in physics space.
        bulletAppState.setDebugEnabled(true);

        // Create a CollisionShape for unit cubes.
        float cubeHalfExtent = 0.5f;
        CollisionShape cubeShape = new BoxCollisionShape(cubeHalfExtent);

        // Create a dynamic body and add it to the space.
        float cubeMass = 1f;
        dynamicCube = new PhysicsRigidBody(cubeShape, cubeMass);
        physicsSpace.addCollisionObject(dynamicCube);
        dynamicCube.setPhysicsLocation(new Vector3f(0f, 4f, 0f));

        // Create a static body and add it to the space.
        staticCube = new PhysicsRigidBody(cubeShape, PhysicsBody.massForStatic);
        physicsSpace.addCollisionObject(staticCube);

        // Minie's BulletAppState simulates the dynamics...
    }
    // *************************************************************************
    // PhysicsTickListener methods

    /**
     * Callback from Bullet, invoked just before the simulation is stepped.
     *
     * @param space the space that is about to be stepped (not null)
     * @param timeStep the time per physics step (in seconds, &ge;0)
     */
    @Override
    public void prePhysicsTick(PhysicsSpace space, float timeStep) {
        // do nothing
    }

    /**
     * Callback from Bullet, invoked just after the simulation has been stepped.
     *
     * @param space ignored
     * @param timeStep ignored
     */
    @Override
    public void physicsTick(PhysicsSpace space, float timeStep) {
        /*
         * Once the dynamic cube gets deactivated,
         * remove the supporting cube from the PhysicsSpace.
         */
        if (!dynamicCube.isActive() && physicsSpace.contains(staticCube)) {
            physicsSpace.removeCollisionObject(staticCube);
        }
    }
}
