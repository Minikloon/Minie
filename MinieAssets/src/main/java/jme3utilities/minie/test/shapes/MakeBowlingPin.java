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
package jme3utilities.minie.test.shapes;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.gltf.GlbLoader;
import com.jme3.system.NativeLibraryLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.Heart;
import jme3utilities.MyString;
import vhacd.VHACD;
import vhacd.VHACDParameters;
import vhacd.VHACDProgressListener;

/**
 * A console application to generate the collision-shape asset "bowlingPin.j3o".
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class MakeBowlingPin {
    // *************************************************************************
    // constants and loggers

    /**
     * which mesh decomposition to use
     */
    final private static boolean useManualDecomposition = true;
    /**
     * message logger for this class
     */
    final private static Logger logger
            = Logger.getLogger(MakeBowlingPin.class.getName());
    /**
     * filesystem path to the asset directory/folder for output
     */
    final private static String assetDirPath
            = "../MinieExamples/src/main/resources";
    // *************************************************************************
    // new methods exposed

    /**
     * Main entry point for the MakeBowlingPin application.
     *
     * @param arguments array of command-line arguments (not null)
     */
    public static void main(String[] arguments) {
        NativeLibraryLoader.loadNativeLibrary("bulletjme", true);
        /*
         * Mute the chatty loggers found in some imported packages.
         */
        Heart.setLoggingLevels(Level.WARNING);
        /*
         * Set the logging level for this class.
         */
        //logger.setLevel(Level.INFO);
        /*
         * Instantiate the application.
         */
        MakeBowlingPin application = new MakeBowlingPin();
        /*
         * Log the working directory.
         */
        String userDir = System.getProperty("user.dir");
        logger.log(Level.INFO, "working directory is {0}",
                MyString.quote(userDir));
        /*
         * Generate the collision shape.
         */
        application.makeBowlingPin();
    }
    // *************************************************************************
    // private methods

    /**
     * Generate a collision shape for a bowling pin.
     */
    private void makeBowlingPin() {
        DesktopAssetManager assetManager = new DesktopAssetManager();
        assetManager.registerLoader(GlbLoader.class, "glb");
        assetManager.registerLoader(J3MLoader.class, "j3md");
        assetManager.registerLocator(null, ClasspathLocator.class);
        /*
         * Import the BowlingPin model (by Stephen Gold)
         * from src/main/resources:
         */
        Spatial cgmRoot
                = assetManager.loadModel("Models/BowlingPin/BowlingPin.glb");
        /*
         * Generate a CollisionShape to approximate the Mesh.
         */
        CompoundCollisionShape shape;
        if (useManualDecomposition) {
            shape = (CompoundCollisionShape) CollisionShapeFactory.createDynamicMeshShape(cgmRoot);
        } else {
            VHACD.addProgressListener(new VHACDProgressListener() {
                double lastOP = -1.0;

                @Override
                public void update(double overallPercent, double stagePercent,
                        double operationPercent, String stageName,
                        String operationName) {
                    if (overallPercent != lastOP) {
                        System.out.printf("MakeBowlingPin %.0f%% complete%n",
                                overallPercent);
                        lastOP = overallPercent;
                    }
                }
            });
            VHACDParameters parms = new VHACDParameters();
            parms.setVoxelResolution(1_300_000);
            shape = CollisionShapeFactory.createVhacdShape(cgmRoot, parms,
                    null);
            if (shape.countChildren() == 0) {
                System.err.println("V-HACD failed!");
                System.exit(-1);
            }
        }
        System.out.printf("MakeBowlingPin number of hulls = %d%n", shape.countChildren());
        /*
         * Write the shape to the asset file.
         */
        String assetPath = "CollisionShapes/bowlingPin.j3o";
        String writeFilePath = String.format("%s/%s", assetDirPath, assetPath);
        Heart.writeJ3O(writeFilePath, shape);
    }
}
