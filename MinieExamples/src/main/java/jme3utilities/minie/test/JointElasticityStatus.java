/*
 Copyright (c) 2020-2022, Stephen Gold
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
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import java.util.Arrays;
import java.util.logging.Logger;
import jme3utilities.SimpleAppState;
import jme3utilities.math.MyArray;
import jme3utilities.math.MyMath;
import jme3utilities.minie.test.common.PhysicsDemo;
import jme3utilities.ui.AbstractDemo;

/**
 * AppState to display the status of the JointElasticity application in an
 * overlay. The overlay consists of status lines, one of which is selected for
 * editing. The overlay is located in the upper-left portion of the display.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class JointElasticityStatus extends SimpleAppState {
    // *************************************************************************
    // constants and loggers

    /**
     * list of constraint-solver iteration counts, in ascending order
     */
    final private static int[] iterationsValues
            = {10, 250, 500, 1000, 2000, 4000};
    /**
     * list of mass ratios, in ascending order
     */
    final private static float[] ratioValues = {1f, 10f, 100f, 1000f};
    /**
     * list of physics timesteps, in ascending order
     */
    final private static float[] timestepValues
            = {0.002f, 0.003f, 0.005f, 0.01f, 1f / 60, 0.04f};
    /**
     * index of the status line for the constraint-solver iteration count
     */
    final private static int iterationsStatusLine = 0;
    /**
     * index of the status line for the mass ratio
     */
    final private static int ratioStatusLine = 1;
    /**
     * index of the status line for physics timestep
     */
    final private static int timestepStatusLine = 2;
    /**
     * number of lines of text in the overlay
     */
    final private static int numStatusLines = 3;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(JointElasticityStatus.class.getName());
    // *************************************************************************
    // fields

    /**
     * lines of text displayed in the upper-left corner of the GUI node ([0] is
     * the top line)
     */
    final private BitmapText[] statusLines = new BitmapText[numStatusLines];
    /**
     * ball's mass as a multiple of the door
     */
    private float massRatio = 1f;
    /**
     * physics timestep
     */
    private float timestep = 1f / 60;
    /**
     * maximum number of solver iterations per physics timestep
     */
    private int numIterations = 10;
    /**
     * index of the line being edited (&ge;1)
     */
    private int selectedLine = timestepStatusLine;
    /**
     * reference to the application instance
     */
    private JointElasticity appInstance;
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized enabled state.
     */
    JointElasticityStatus() {
        super(true);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Advance the field selection by the specified amount.
     *
     * @param amount the number of fields to move downward
     */
    void advanceSelectedField(int amount) {
        int firstField = 0;
        int numFields = numStatusLines - firstField;

        int selectedField = selectedLine - firstField;
        int sum = selectedField + amount;
        selectedField = MyMath.modulo(sum, numFields);
        selectedLine = selectedField + firstField;
    }

    /**
     * Advance the value of the selected field by the specified amount.
     *
     * @param amount
     */
    void advanceValue(int amount) {
        switch (selectedLine) {
            case iterationsStatusLine:
                advanceIterations(amount);
                break;

            case ratioStatusLine:
                advanceRatio(amount);
                break;

            case timestepStatusLine:
                advanceTimestep(amount);
                break;

            default:
                throw new IllegalStateException("line = " + selectedLine);
        }
    }

    float massRatio() {
        assert massRatio > 0f : massRatio;
        return massRatio;
    }

    int numIterations() {
        assert numIterations > 0 : numIterations;
        return numIterations;
    }

    float timeStep() {
        assert timestep > 0f : timestep;
        return timestep;
    }
    // *************************************************************************
    // ActionAppState methods

    /**
     * Clean up this AppState during the first update after it gets detached.
     * Should be invoked only by a subclass or by the AppStateManager.
     */
    @Override
    public void cleanup() {
        super.cleanup();
        /*
         * Remove the status lines from the guiNode.
         */
        for (int i = 0; i < numStatusLines; ++i) {
            statusLines[i].removeFromParent();
        }
    }

    /**
     * Initialize this AppState on the first update after it gets attached.
     *
     * @param sm application's state manager (not null)
     * @param app application which owns this state (not null)
     */
    @Override
    public void initialize(AppStateManager sm, Application app) {
        super.initialize(sm, app);

        appInstance = (JointElasticity) app;
        BitmapFont guiFont
                = assetManager.loadFont("Interface/Fonts/Default.fnt");
        /*
         * Add the status lines to the guiNode.
         */
        for (int lineIndex = 0; lineIndex < numStatusLines; ++lineIndex) {
            statusLines[lineIndex] = new BitmapText(guiFont);
            float y = cam.getHeight() - 20f * lineIndex;
            statusLines[lineIndex].setLocalTranslation(0f, y, 0f);
            guiNode.attachChild(statusLines[lineIndex]);
        }

        assert MyArray.isSorted(iterationsValues);
        assert MyArray.isSorted(ratioValues);
        assert MyArray.isSorted(timestepValues);
    }

    /**
     * Callback to update this AppState prior to rendering. (Invoked once per
     * frame while the state is attached and enabled.)
     *
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);

        PhysicsSpace space = appInstance.getPhysicsSpace();
        int index = 1 + Arrays.binarySearch(iterationsValues, numIterations);
        String message = String.format("Solver iterations (#%d of %d):   %d",
                index, iterationsValues.length, numIterations);
        updateStatusLine(iterationsStatusLine, message);

        index = 1 + Arrays.binarySearch(ratioValues, massRatio);
        message = String.format("Mass ratio (#%d of %d):   %.0f : 1",
                index, ratioValues.length, massRatio);
        updateStatusLine(ratioStatusLine, message);

        index = 1 + Arrays.binarySearch(timestepValues, timestep);
        message = String.format("Timestep (#%d of %d):   %.4f second",
                index, timestepValues.length, timestep);
        updateStatusLine(timestepStatusLine, message);
    }
    // *************************************************************************
    // private methods

    /**
     * Advance the iteration-count selection by the specified amount.
     *
     * @param amount the number of values to advance (may be negative)
     */
    private void advanceIterations(int amount) {
        numIterations = AbstractDemo.advanceInt(iterationsValues,
                numIterations, amount);
        PhysicsSpace physicsSpace = appInstance.getPhysicsSpace();
        physicsSpace.getSolverInfo().setNumIterations(numIterations);
    }

    /**
     * Advance the mass ratio by the specified amount.
     *
     * @param amount the number of values to advance (may be negative)
     */
    private void advanceRatio(int amount) {
        massRatio = PhysicsDemo.advanceFloat(ratioValues, massRatio, amount);
        appInstance.setMassRatio(massRatio);
    }

    /**
     * Advance the timestep selection by the specified amount.
     *
     * @param amount the number of values to advance (may be negative)
     */
    private void advanceTimestep(int amount) {
        timestep = PhysicsDemo.advanceFloat(timestepValues, timestep, amount);
        PhysicsSpace physicsSpace = appInstance.getPhysicsSpace();
        physicsSpace.setAccuracy(timestep);
    }

    /**
     * Update the indexed status line.
     */
    private void updateStatusLine(int lineIndex, String text) {
        BitmapText spatial = statusLines[lineIndex];

        if (lineIndex == selectedLine) {
            spatial.setColor(ColorRGBA.Yellow);
            spatial.setText("-> " + text);
        } else {
            spatial.setColor(ColorRGBA.White);
            spatial.setText(" " + text);
        }
    }
}
