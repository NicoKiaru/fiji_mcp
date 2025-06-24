package sc.fiji.tools;

import com.google.gson.GsonBuilder;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import net.imagej.ImageJ;
import org.scijava.module.ModuleService;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptModule;
import org.scijava.ui.swing.script.EditorPane;
import org.scijava.ui.swing.script.TextEditor;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FijiTools {
    TextEditor textEditor;

    public FijiTools(ImageJ ij) {
        this.ij = ij;
        textEditor = new TextEditor(ij.context());
        textEditor.setVisible(true);
    }

    final ImageJ ij;

    public String executeGroovy(String code) {

        String extractedCode = extractCode(code);
        //System.out.println("EXECUTING\n"+extractedCode);

        ScriptInfo si = new ScriptInfo(ij.context(),"dummy.groovy",new StringReader(extractedCode));

        ScriptModule sm = (ScriptModule) ij.get(ModuleService.class).createModule(si);
        StringWriter sw = new StringWriter();
        sm.setErrorWriter(sw);

        ExecutionResult result = new ExecutionResult();

        Object o = null;
        try {

            o = ij.get(ModuleService.class).run(sm, true).get().getReturnValue();

            if (!sw.toString().isEmpty()) {
                result.errorMessage = sw.toString();
                result.executionSuccess = false;
                result.returnedObject = null;
                result.returnedObjectClass = "null";
                System.out.println("RETURNED:\n"+
                        new GsonBuilder().setPrettyPrinting().create().toJson(result));
                return new GsonBuilder().setPrettyPrinting().create().toJson(result);
            }

            result.errorMessage = null;
            result.executionSuccess = true;
            result.returnedObject = o;
            if (o!=null) {
                result.returnedObjectClass = o.getClass().getName();
            } else {
                result.returnedObjectClass = "null";
            }
            System.out.println("RETURNED:\n"+
                    new GsonBuilder().setPrettyPrinting().create().toJson(result));
            return new GsonBuilder().setPrettyPrinting().create().toJson(result);
        } catch (Exception ex) {
            result.errorMessage = sw.toString();
            result.executionSuccess = false;
            result.returnedObject = null;
            result.returnedObjectClass = "null";
            System.out.println("RETURNED:\n"+
                    new GsonBuilder().setPrettyPrinting().create().toJson(result));
            return new GsonBuilder().setPrettyPrinting().create().toJson(result);
        }
    }

    public void showOrUpdateScriptInEditor(String code, String scriptTitle) {
        boolean error = false;
        int index = 0;
        EditorPane pane = null;

        while ((!error)&&(pane == null)) {
            try {
                EditorPane testpane = textEditor.getEditorPane(index);

                Field f = EditorPane.class.getDeclaredField("fallBackBaseName");
                f.setAccessible(true);

                String title = (String) f.get(testpane);//testpane.fallBackBaseName;

                if (title.replace(".groovy","").equals(scriptTitle.replace(".groovy", ""))) {
                    pane = testpane;
                }
                index ++;

            } catch (Exception e) {
                error = true;
            }
        }

        if (pane == null) {
            //return "Couldn't get script "+scriptTitle+" maybe its title has changed or it has been closed ?";
            textEditor.createNewDocument(scriptTitle, code);
        } else {
            pane.setText(code);
            //System.out.println(pane.getText());
            //return pane.getText();
        }
    }

    public String getScriptFromEditor(String scriptTitle) {
        boolean error = false;
        int index = 0;
        EditorPane pane = null;

        while ((!error)&&(pane == null)) {
            try {
                EditorPane testpane = textEditor.getEditorPane(index);

                Field f = EditorPane.class.getDeclaredField("fallBackBaseName");
                f.setAccessible(true);

                String title = (String) f.get(testpane);//testpane.fallBackBaseName;

                if (title.replace(".groovy","").equals(scriptTitle.replace(".groovy", ""))) {
                    pane = testpane;
                }
                index ++;

            } catch (Exception e) {
                error = true;
            }
        }

        if (pane == null) {
            return new GsonBuilder().setPrettyPrinting().create().toJson("ERROR! Couldn't get script "+scriptTitle+" maybe its title has changed or it has been closed ?");
        } else {
            return new GsonBuilder().setPrettyPrinting().create().toJson(pane.getText());
        }
    }

    public String fetchSourceCodeContent(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return ImageJInvestigator.getSourceCode(clazz, this.ij.context());
        } catch (ClassNotFoundException e) {
            return "ERROR! Class not found";
        }
    }

    public static String extractCode(String code) {
        if (code.startsWith("```groovy")) {
            code = code.substring(10);
        }

        if (code.endsWith("```")) {
            code = code.substring(0,code.length()-3);
        }
        return code;
    }

    public static class ExecutionResult {
        public String returnedObjectClass;
        public Object returnedObject;
        public boolean executionSuccess;
        public String errorMessage;
    }

    public String getCurrentState() {
        List<ImagePlusDescription> images =
                Arrays.stream(WindowManager.getImageTitles() ).map(WindowManager::getImage)
                        .map(FijiTools::getImageDescription).collect(Collectors.toList());

        return new GsonBuilder().setPrettyPrinting().create().toJson(images);
    }

    public static class ImagePlusDescription {
        String title;
        boolean is_the_active_one;
        int x_size_pix;
        int y_size_pix;
        int z_size_pix;
        int n_channels;
        int n_timepoints;
        int current_active_channel;
        int current_active_zslice;
        int current_active_timepoint;
    }

    public static ImagePlusDescription getImageDescription(ImagePlus imp) {
        ImagePlusDescription desc = new ImagePlusDescription();
        desc.x_size_pix = imp.getWidth();
        desc.y_size_pix = imp.getHeight();
        desc.z_size_pix = imp.getNSlices();
        desc.n_channels = imp.getNChannels();
        desc.n_timepoints = imp.getNFrames();
        desc.title = imp.getTitle();
        desc.is_the_active_one = imp.equals(IJ.getImage());
        desc.current_active_channel = imp.getC();
        desc.current_active_zslice = imp.getZ();
        desc.current_active_timepoint = imp.getT();
        return desc;
    }
}
