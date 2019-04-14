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
package jme3utilities.minie.wizard;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.animation.CenterHeuristic;
import com.jme3.bullet.animation.ShapeHeuristic;
import com.jme3.cursors.plugins.JmeCursor;
import com.jme3.input.KeyInput;
import com.jme3.math.Vector3f;
import java.util.logging.Level;
import java.util.logging.Logger;
import jme3utilities.MyString;
import jme3utilities.Validate;
import jme3utilities.math.MyVector3f;
import jme3utilities.ui.InputMode;

/**
 * Input mode for the "links" screen of DacWizard.
 *
 * @author Stephen Gold sgold@sonic.net
 */
class LinksMode extends InputMode {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final static Logger logger = Logger.getLogger(LinksMode.class.getName());
    /**
     * asset path to the cursor for this mode
     */
    final private static String assetPath = "Textures/cursors/default.cur";
    // *************************************************************************
    // constructors

    /**
     * Instantiate a disabled, uninitialized mode.
     */
    LinksMode() {
        super("links");
    }
    // *************************************************************************
    // InputMode methods

    /**
     * Add default hotkey bindings. These bindings will be used if no custom
     * bindings are found.
     */
    @Override
    protected void defaultBindings() {
        bind(SimpleApplication.INPUT_MAPPING_EXIT, KeyInput.KEY_ESCAPE);
        bind(Action.editBindings, KeyInput.KEY_F1);
        bind(Action.editDisplaySettings, KeyInput.KEY_F2);

        bind(Action.dumpPhysicsSpace, KeyInput.KEY_O);
        bind(Action.dumpRenderer, KeyInput.KEY_P);
    }

    /**
     * Initialize this (disabled) mode prior to its 1st update.
     *
     * @param stateManager (not null)
     * @param application (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager,
            Application application) {
        /*
         * Set the mouse cursor for this mode.
         */
        AssetManager manager = application.getAssetManager();
        JmeCursor cursor = (JmeCursor) manager.loadAsset(assetPath);
        setCursor(cursor);

        super.initialize(stateManager, application);
    }

    /**
     * Process an action from the keyboard or mouse.
     *
     * @param actionString textual description of the action (not null)
     * @param ongoing true if the action is ongoing, otherwise false
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void onAction(String actionString, boolean ongoing, float tpf) {
        Validate.nonNull(actionString, "action string");
        logger.log(Level.INFO, "Got action {0} ongoing={1}", new Object[]{
            MyString.quote(actionString), ongoing
        });

        boolean handled = false;
        if (ongoing) {
            LinksScreen screen = DacWizard.findAppState(LinksScreen.class);
            switch (actionString) {
                case Action.nextMassHeuristic:
                    screen.nextMassHeuristic();
                    handled = true;
                    break;

                case Action.nextScreen:
                    nextScreen();
                    handled = true;
                    break;

                case Action.previousScreen:
                    previousScreen();
                    handled = true;
                    break;

                case Action.selectCenterHeuristic:
                    screen.selectCenterHeuristic();
                    handled = true;
                    break;

                case Action.selectShapeHeuristic:
                    screen.selectShapeHeuristic();
                    handled = true;
                    break;

                case Action.setMassParameter:
                    screen.setMassParameter();
                    handled = true;
                    break;

                case Action.setShapeScale:
                    screen.setShapeScale();
                    handled = true;
                    break;
            }
            if (!handled) {
                handled = testForPrefixes(actionString);
            }
        }
        if (!handled) {
            actionApplication.onAction(actionString, ongoing, tpf);
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Advance to the TestScreen.
     */
    private void nextScreen() {
        setEnabled(false);
        InputMode test = InputMode.findMode("test");
        test.setEnabled(true);
    }

    /**
     * Go back to the BonesScreen.
     */
    private void previousScreen() {
        setEnabled(false);
        InputMode bones = InputMode.findMode("bones");
        bones.setEnabled(true);
    }

    /**
     * Test an ongoing action for prefixes.
     *
     * @param actionString textual description of the action (not null)
     * @return true if the action is handled, otherwise false
     */
    private boolean testForPrefixes(String actionString) {
        boolean handled = true;
        LinksScreen screen = DacWizard.findAppState(LinksScreen.class);
        String arg;

        if (actionString.startsWith("select centerHeuristic ")) {
            arg = MyString.remainder(actionString, "select centerHeuristic ");
            CenterHeuristic heuristic = CenterHeuristic.valueOf(arg);
            screen.selectCenterHeuristic(heuristic);

        } else if (actionString.startsWith("select shapeHeuristic ")) {
            arg = MyString.remainder(actionString, "select shapeHeuristic ");
            ShapeHeuristic heuristic = ShapeHeuristic.valueOf(arg);
            screen.selectShapeHeuristic(heuristic);

        } else if (actionString.startsWith("set massParameter ")) {
            arg = MyString.remainder(actionString, "set massParameter ");
            float value = Float.parseFloat(arg);
            screen.setMassParameter(value);

        } else if (actionString.startsWith("set shapeScale ")) {
            arg = MyString.remainder(actionString, "set shapeScale ");
            Vector3f scaleFactors = MyVector3f.parse(arg);
            screen.setShapeScale(scaleFactors);

        } else {
            handled = false;
        }

        return handled;
    }
}
