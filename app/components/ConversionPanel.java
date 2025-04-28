import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ConversionPanel extends JPanel {
    public ConversionPanel() {
        // Set up key bindings for [ and ]
        InputMap inputMap = this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = this.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke('['), "previousConversion");
        inputMap.put(KeyStroke.getKeyStroke(']'), "nextConversion");

        actionMap.put("previousConversion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onPreviousConversion();
            }
        });
        actionMap.put("nextConversion", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onNextConversion();
            }
        });

        this.setFocusable(true);
        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyChar() == '[') {
                    onPreviousConversion();
                } else if (e.getKeyChar() == ']') {
                    onNextConversion();
                }
            }
        });
    }

    private void onPreviousConversion() {
        // ... your logic for previous conversion ...
    }

    private void onNextConversion() {
        // ... your logic for next conversion ...
    }
} 