package sc.fiji.tools;

import org.reflections.Reflections;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.Service;
import org.scijava.widget.Button;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BuildMDForLLM {

    public static void main(String... args) {
        System.out.println(getCommandsInfos("sc.fiji.bdvpg"));

        //System.out.println(getCommandsInfos("ch.epfl.biop"));
    }


    private static Field[] filterSkippable(Field[] declaredFields) {
        return Arrays.stream(declaredFields)
                .filter((f) -> {
                    if (Service.class.isAssignableFrom(f.getType())) {
                        return false;
                    }
                    if (f.getType().equals(Context.class)) {
                        return false;
                    }
                    if (f.getType().equals(Button.class)) {
                        return false;
                    }
                    return true;
                }).toArray(Field[]::new);
    }


    public static String getCommandsInfos(String packagePath) {

        Reflections reflections = new Reflections(packagePath);

        //Set<Class<? extends Command>> commandClasses =
        //        reflections.getSubTypesOf(Command.class);


        Set<Class<? extends Command>> commandClasses =
                reflections.getSubTypesOf(Command.class)
                        .stream()
                        .filter(clazz -> !(InteractiveCommand.class.isAssignableFrom(clazz)))
                        .filter(clazz -> !(DynamicCommand.class.isAssignableFrom(clazz)))
                        .collect(Collectors.toSet());

        HashMap<String, String> docPerClass = new HashMap<>();

        commandClasses.forEach(c -> {
            //System.out.println(c.getName());

            StringBuilder infos = new StringBuilder();

            Plugin plugin = c.getAnnotation(Plugin.class);
            if (plugin!=null) {
                //String url = linkGitHubRepoPrefix+c.getName().replaceAll("\\.","\\/")+".java";
                infos.append("# " + c.getName() + "\n");
                if (!plugin.label().isEmpty()) {
                    infos.append("Label: " + plugin.label() + "\n");
                }
                if (!plugin.description().isEmpty()) {
                    infos.append("Description: " + plugin.description() + "\n");
                }

                //Field[] fields = c.getDeclaredFields();


                List<Field> allFields = new ArrayList<>();
                allFields.addAll(Arrays.asList(filterSkippable(c.getDeclaredFields())));
                allFields.addAll(Arrays.asList(filterSkippable(c.getSuperclass().getDeclaredFields())));

                List<Field> inputFields = allFields.stream()
                        .filter(f -> f.isAnnotationPresent(Parameter.class))
                        .filter(f -> {
                            Parameter p = f.getAnnotation(Parameter.class);
                            return (p.type() == ItemIO.INPUT) || (p.type() == ItemIO.BOTH);
                        })
                        .filter(f -> {
                            Parameter p = f.getAnnotation(Parameter.class);
                            return (p.visibility() != ItemVisibility.MESSAGE);
                        }).
                        sorted(Comparator.comparing(Field::getName)).collect(Collectors.toList());
                if (!inputFields.isEmpty()) {
                    //DeepSliceFolderCommand
                    infos.append("## Input\n");
                    inputFields.forEach(f -> {
                        infos.append(f.getType().getSimpleName()+" " + f.getName() + ";");
                        if ((!f.getAnnotation(Parameter.class).label().isEmpty())||(!f.getAnnotation(Parameter.class).description().isEmpty())) {
                            infos.append(" //");
                        }
                        if (f.getAnnotation(Parameter.class).label().isEmpty()) {
                        } else {
                            infos.append(" Label: "+f.getAnnotation(Parameter.class).label() + ";");
                        }
                        if (!f.getAnnotation(Parameter.class).description().isEmpty()) {
                            infos.append(" Description: "+f.getAnnotation(Parameter.class).description() + ";");
                        }
                        infos.append("\n");
                    });
                } else {
                    infos.append("## Input\n");
                }

                List<Field> outputFields = allFields.stream()
                        .filter(f -> f.isAnnotationPresent(Parameter.class))
                        .filter(f -> {
                            Parameter p = f.getAnnotation(Parameter.class);
                            return (p.type() == ItemIO.OUTPUT) || (p.type() == ItemIO.BOTH);
                        }).sorted(Comparator.comparing(Field::getName)).collect(Collectors.toList());
                if (!outputFields.isEmpty()) {
                    infos.append("## Output\n");
                    outputFields.forEach(f -> {
                        /*infos.append(f.getType().getSimpleName()+" " + f.getName() + ";");
                        if (f.getAnnotation(Parameter.class).label().isEmpty()) {
                            infos.append("\n");
                        } else {
                            infos.append(" // "+f.getAnnotation(Parameter.class).label() + "\n");
                        }
                        if (!f.getAnnotation(Parameter.class).description().isEmpty()) {
                            infos.append(f.getAnnotation(Parameter.class).description() + "\n");
                        }*/
                        if ((!f.getAnnotation(Parameter.class).label().isEmpty())||(!f.getAnnotation(Parameter.class).description().isEmpty())) {
                            infos.append(" //");
                        }
                        if (f.getAnnotation(Parameter.class).label().isEmpty()) {
                        } else {
                            infos.append(" Label: "+f.getAnnotation(Parameter.class).label() + ";");
                        }
                        if (!f.getAnnotation(Parameter.class).description().isEmpty()) {
                            infos.append(" Description: "+f.getAnnotation(Parameter.class).description() + ";");
                        }
                        infos.append("\n");
                    });
                } else {
                    infos.append("## Output\n");
                }

                infos.append("\n");

                docPerClass.put(c.getName(),infos.toString());
            }
        });

        StringBuilder concat = new StringBuilder();

        Object[] keys = docPerClass.keySet().toArray();
        Arrays.sort(keys);
        for (Object key:keys) {
            String k = (String) key;
            concat.append(docPerClass.get(k));
        }

        return concat.toString();
    }

}