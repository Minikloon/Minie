/*
 Copyright (c) 2019, Stephen Gold
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
import com.jme3.bullet.PhysicsSoftSpace;
import com.jme3.bullet.SoftPhysicsAppState;
import com.jme3.bullet.objects.PhysicsSoftBody;
import com.jme3.bullet.objects.infos.ConfigFlag;
import com.jme3.bullet.objects.infos.Sbcp;
import com.jme3.bullet.objects.infos.SoftBodyConfig;
import com.jme3.bullet.util.NativeSoftBodyUtil;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import jme3utilities.minie.test.mesh.Icosphere;

/**
 * A simple example of a soft-soft collision.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class HelloSoftSoft extends SimpleApplication {
    // *************************************************************************
    // fields

    /**
     * PhysicsSpace for simulation
     */
    private PhysicsSoftSpace physicsSpace;
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the HelloSoftSoft application.
     *
     * @param ignored array of command-line arguments (not null)
     */
    public static void main(String[] ignored) {
        HelloSoftSoft application = new HelloSoftSoft();
        application.start();
    }
    // *************************************************************************
    // SimpleApplication methods

    /**
     * Initialize this application.
     */
    @Override
    public void simpleInitApp() {
        // Set up Bullet physics (with debug enabled).
        SoftPhysicsAppState bulletAppState = new SoftPhysicsAppState();
        bulletAppState.setDebugEnabled(true); // default = false
        stateManager.attach(bulletAppState);
        physicsSpace = bulletAppState.getPhysicsSoftSpace();

        // Set gravity to zero.
        physicsSpace.setGravity(Vector3f.ZERO); // default = default=(0,-9.81,0)

        // Relocate the camera.
        cam.setLocation(new Vector3f(0f, 1f, 8f));

        // A mesh is used to generate the shape and topology
        // for each soft body.
        int numRefinementIterations = 3;
        float radius = 1f;
        Mesh icosphere = new Icosphere(numRefinementIterations, radius);

        // Create 2 squishy balls and add them to the physics space.
        PhysicsSoftBody ball1 = new PhysicsSoftBody();
        NativeSoftBodyUtil.appendFromTriMesh(icosphere, ball1);
        physicsSpace.add(ball1);

        PhysicsSoftBody ball2 = new PhysicsSoftBody();
        NativeSoftBodyUtil.appendFromTriMesh(icosphere, ball2);
        physicsSpace.add(ball2);

        // Set each ball's default frame pose:  if deformed,
        // it will tend to return to its current shape.
        boolean setVolumePose = false;
        boolean setFramePose = true;
        ball1.setPose(setVolumePose, setFramePose);
        ball2.setPose(setVolumePose, setFramePose);

        // Make each ball bouncy by enabling pose matching.
        SoftBodyConfig config1 = ball1.getSoftConfig();
        config1.set(Sbcp.PoseMatching, 0.5f); // default = 0
        SoftBodyConfig config2 = ball2.getSoftConfig();
        config2.set(Sbcp.PoseMatching, 0.5f);

        // Enable soft-soft collisions for each ball.
        // Clearing all other collision flags disables soft-rigid collisions.
        config1.setCollisionFlags(ConfigFlag.VF_SS); // default = SDF_RS
        config2.setCollisionFlags(ConfigFlag.VF_SS);

        // Translate ball2 upward and put it on a collision course with ball1.
        ball2.applyTranslation(new Vector3f(0f, 3f, 0f));
        ball2.setVelocity(new Vector3f(0f, -1f, 0f));
    }
}
