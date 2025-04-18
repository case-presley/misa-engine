package misa.systems.input;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class to handle keyboard input and set up key bindings.
 * Supports Lua scripting for defining dynamic key actions.
 */
@SuppressWarnings("unused")
public class InputHandler
{
    private static final Logger LOGGER = Logger.getLogger(InputHandler.class.getName());

    /**
     * Sets up key bindings so that the provided actions will trigger on key press and release events.
     *
     * @param component The JComponent to attach key bindings to.
     * @param key       The key for which the actions will be set.
     * @param onPress   The action to execute when the key is pressed.
     * @param onRelease The action to execute when the key is released.
     */
    public void setupKeyBindings(JComponent component, String key,
                                 Runnable onPress, Runnable onRelease)
    {
        if (component == null || key == null || onPress == null || onRelease == null)
        {
            LOGGER.warning("Invalid input parameters... " +
                    "component, key, onPress, and onRelease cannot be null.");
            return;
        }

        // Setup key press action
        String pressActionKey = key + "Press";
        setupKeyAction(component, KeyStroke.getKeyStroke(key), pressActionKey, onPress, true);

        // Setup key release action
        String releaseActionKey = key + "Release";
        setupKeyAction(component, KeyStroke.getKeyStroke("released " + key), releaseActionKey, onRelease, false);
    }

    /**
     * Helper to set up a key action.
     *
     * @param component The JComponent to attach the action to.
     * @param keyStroke The keystroke that triggers the action.
     * @param actionKey The identifier for the action.
     * @param action    The runnable action that executes when triggered.
     * @param isPressed True if the action is key press, false if the action is key release.
     */
    private void setupKeyAction(JComponent component, KeyStroke keyStroke,
                                String actionKey, Runnable action, boolean isPressed)
    {
        if (keyStroke == null)
        {
            LOGGER.warning("Invalid KeyStroke for action: " + actionKey);
            return;
        }

        // Bind the key action to the component
        component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionKey);
        component.getActionMap().put(actionKey, new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                action.run();
            }
        });

        String eventType = isPressed ? "Press" : "Release";
        LOGGER.log(Level.INFO, "Key binding is set up: {0} ({1})",
                new Object[]{actionKey, eventType});
    }
}
