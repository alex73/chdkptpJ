
/**************************************************************************
 chdkptpJ - Java CHDK PTP framework.

 Copyright (C) 2015 Aleś Bułojčyk (alex73mail@gmail.com)

 This file is part of chdkptpJ.

 chdkptpJ is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 chdkptpJ is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.alex73.chdkptpj.camera.Camera;
import org.alex73.chdkptpj.camera.CameraFactory;
import org.apache.log4j.PropertyConfigurator;

/**
 * UI for show changed UI Properties.
 */
public class TestUIPropertiesUI extends JFrame {

    public static void main(String[] args) {
        //PropertyConfigurator.configure("log4j.properties");
        new TestUIPropertiesUI().setVisible(true);
    }

    private final JButton btnRun;
    private final JTextArea text;
    private final JTextField tFrom, tTo;
    private JScrollPane sc;
    private Camera c;
    private Map<String, String> initial;
    private List<String> initialSorted;

    public TestUIPropertiesUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 400);
        setTitle("UI Properties checker");

        tFrom = new JTextField("0x8001");
        tTo = new JTextField("0x80c5");
        btnRun = new JButton("Connect");
        text = new JTextArea();
        text.setEditable(false);
        sc = new JScrollPane(text);
        tFrom.setColumns(10);
        tTo.setColumns(10);

        setLayout(new BorderLayout());

        JPanel upper = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0);

        gbc.gridx = 0;
        gbc.gridy = 0;
        upper.add(new JLabel("UI prop from: "), gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        upper.add(new JLabel("UI prop to: "), gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        upper.add(tFrom, gbc);
        gbc.gridx = 1;
        gbc.gridy = 1;
        upper.add(tTo, gbc);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        upper.add(btnRun, gbc);
        add(upper, BorderLayout.NORTH);

        add(sc, BorderLayout.CENTER);

        btnRun.addActionListener(run);

        show("This application displays changes in the UI Properties (PTM functions)\n\n"
                + "Press 'Connect' button for connect to camera");
    }

    void show(String message) {
        text.setText(message);
        text.setCaretPosition(0);
        sc.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
    }

    ActionListener run = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (c == null) {
                connect();
            } else if (initial == null) {
                remember();
            } else {
                check();
            }
        }
    };

    void connect() {
        try {
            List<Camera> cameras = CameraFactory.findCameras();
            switch (cameras.size()) {
            case 0:
                text.setText("There is no connected camera");
                return;
            case 1:
                break;
            default:
                text.setText("Too many connected cameras - need only one");
                return;
            }
            c = cameras.get(0);
            c.connect();
            c.setRecordMode();

            btnRun.setText("Remember");
            show("Connected camera: " + c.getDevice().getManufacturerString() + "/"
                    + c.getDevice().getProductString() + " : " + c.getDevice().getSerialNumberString()
                    + "\n\n"
                    + "Set from/to UI properties indexes above. If you will set wrong values, camera can hang.\n"
                    + "Then setup your camera into initial state, then press 'Remember' button for request initial properties.");
        } catch (Throwable ex) {
            show("ERROR:" + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            c = null;
        }
    }

    void remember() {
        try {
            initial = requestUIProperties();
            initialSorted = new ArrayList<>(initial.keySet());
            Collections.sort(initialSorted);

            StringBuilder s = new StringBuilder();
            s.append("Initial properties requested.\n");
            s.append(
                    "Change camera settings, then press 'Check' button for see changed properties(against initial)\n\n");
            s.append("Initial values are:\n");
            for (String k : initialSorted) {
                s.append("    " + k + " = " + initial.get(k) + "\n");
            }
            btnRun.setText("Check");
            show(s.toString());
        } catch (Throwable ex) {
            show("ERROR:" + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            initial = null;
        }
    }

    void check() {
        try {
            StringBuilder s = new StringBuilder();

            Map<String, String> changed = requestUIProperties();
            for (String k : initialSorted) {
                String ov = initial.get(k);
                String nv = changed.get(k);
                if (!ov.equals(nv)) {
                    s.append("    " + k + " = " + nv + " (initial was " + ov + ")\n");
                }
            }

            s.append(
                    "\nChange camera settings, then press 'Check' button for see changed properties(against initial)\n");
            show(s.toString());
        } catch (Throwable ex) {
            show("ERROR:" + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    Map<String, String> requestUIProperties() throws Exception {
        String script = "call_event_proc('UI.CreatePublic')\n" + "local r = {}\n" + "for i=" + tFrom.getText()
                + "," + tTo.getText() + " do\n" + "  local v = call_event_proc('PTM_GetCurrentItem',i)\n"
                + "  if v == 65535 then\n" + "    v = -1\n" + "  end\n"
                + "   r[string.format('0x%x',i)] = v\n" + "end\n" + "return r";
        String r = (String) c.executeLua(script);

        Map<String, String> result = new HashMap<>();
        for (String vs : r.split("\n")) {
            String[] kv = vs.split("\t");
            if (kv.length != 2) {
                throw new Exception("Error parse camera response line: '" + vs + "'");
            }
            result.put(kv[0], kv[1]);
        }
        return result;
    }
}
