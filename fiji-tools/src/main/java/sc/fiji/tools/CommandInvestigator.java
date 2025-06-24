package sc.fiji.tools;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.command.InteractiveCommand;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.search.SourceFinder;
import org.scijava.search.SourceNotFoundException;
import org.scijava.service.Service;
import org.scijava.widget.Button;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class CommandInvestigator {


    public static void main(String[] args) {
        List<Class<? extends Command>> abbaClasses = getCommandsFromPackage("ch.epfl.biop.atlas.aligner.command");
        abbaClasses.forEach(System.out::println);
    }

    public static List<Class<? extends Command>> getCommandsFromPackage(String packagePath) {
        Reflections reflections = new Reflections(packagePath);
        return
                reflections.getSubTypesOf(Command.class)
                        .stream()
                        .filter(clazz -> !(InteractiveCommand.class.isAssignableFrom(clazz)))
                        .filter(clazz -> !(DynamicCommand.class.isAssignableFrom(clazz)))
                        .collect(Collectors.toList());
    }

    public static String readContentFromURL(URL url) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    public static String toJson(Class<? extends Command> commandClass) {
        JsonArray jsonArray = new JsonArray();
        Plugin plugin = commandClass.getAnnotation(Plugin.class);
        if (plugin != null) {
            JsonObject jsonObject = new JsonObject();

            // Add the name and description first
            jsonObject.addProperty("name", commandClass.getName());
            if (!plugin.description().isEmpty()) {
                jsonObject.addProperty("description", plugin.description());
            }

            // Collect all fields from the class and its superclass
            List<Field> allFields = new ArrayList<>();
            allFields.addAll(Arrays.asList(filterSkippable(commandClass.getDeclaredFields())));
            allFields.addAll(Arrays.asList(filterSkippable(commandClass.getSuperclass().getDeclaredFields())));

            // Filter and sort input fields
            List<Field> inputFields = allFields.stream()
                    .filter(f -> f.isAnnotationPresent(Parameter.class))
                    .filter(f -> {
                        Parameter p = f.getAnnotation(Parameter.class);
                        return (p.type() == ItemIO.INPUT) || (p.type() == ItemIO.BOTH);
                    })
                    .filter(f -> {
                        Parameter p = f.getAnnotation(Parameter.class);
                        return (p.visibility() != ItemVisibility.MESSAGE);
                    })
                    .sorted(Comparator.comparing(Field::getName))
                    .collect(Collectors.toList());

            // Add input fields to the JSON object
            JsonArray inputArray = new JsonArray();
            inputFields.forEach(f -> {
                JsonObject fieldNode = new JsonObject();
                fieldNode.addProperty("type", f.getType().getSimpleName());
                fieldNode.addProperty("name", f.getName());
                Parameter param = f.getAnnotation(Parameter.class);
                if (!param.label().isEmpty()) {
                    fieldNode.addProperty("label", param.label());
                }
                if (!param.description().isEmpty()) {
                    fieldNode.addProperty("description", param.description());
                }
                inputArray.add(fieldNode);
            });

            jsonObject.add("input", inputArray);

            // Filter and sort output fields
            List<Field> outputFields = allFields.stream()
                    .filter(f -> f.isAnnotationPresent(Parameter.class))
                    .filter(f -> {
                        Parameter p = f.getAnnotation(Parameter.class);
                        return (p.type() == ItemIO.OUTPUT) || (p.type() == ItemIO.BOTH);
                    })
                    .sorted(Comparator.comparing(Field::getName))
                    .collect(Collectors.toList());

            // Add output fields to the JSON object
            JsonArray outputArray = new JsonArray();
            outputFields.forEach(f -> {
                JsonObject fieldNode = new JsonObject();
                fieldNode.addProperty("type", f.getType().getSimpleName());
                fieldNode.addProperty("name", f.getName());
                Parameter param = f.getAnnotation(Parameter.class);
                if (!param.label().isEmpty()) {
                    fieldNode.addProperty("label", param.label());
                }
                if (!param.description().isEmpty()) {
                    fieldNode.addProperty("description", param.description());
                }
                outputArray.add(fieldNode);
            });

            jsonObject.add("output", outputArray);

            // Add the JSON object to the array
            jsonArray.add(jsonObject);
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(jsonArray);
    }

    /**
     * @param commandClass
     * @return a summary of a SciJava command as a piece of markdown formatted text
     */
    public static String toMd(Class<? extends Command> commandClass) {

        StringBuilder infos = new StringBuilder();

        Plugin plugin = commandClass.getAnnotation(Plugin.class);
        if (plugin!=null) {
            //String url = linkGitHubRepoPrefix+c.getName().replaceAll("\\.","\\/")+".java";
            infos.append("# " + commandClass.getName() + "\n");
            if (!plugin.label().isEmpty()) {
                infos.append("Label: " + plugin.label() + "\n");
            }
            if (!plugin.description().isEmpty()) {
                infos.append("Description: " + plugin.description() + "\n");
            }

            List<Field> allFields = new ArrayList<>();
            allFields.addAll(Arrays.asList(filterSkippable(commandClass.getDeclaredFields())));
            allFields.addAll(Arrays.asList(filterSkippable(commandClass.getSuperclass().getDeclaredFields())));

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
        }
        return infos.toString();
    }

    public static String getSourceCode(Class<? extends Command> commandClass, Context context) {
        try {
            URL location = SourceFinder.sourceLocation(commandClass, context.getService(LogService.class));
            return readContentFromURL(convertToRawURL(location));
        } catch (SourceNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static URL convertToRawURL(URL githubURL) throws MalformedURLException {
        // Convert the URL to a string for manipulation
        String urlString = githubURL.toString();

        // Check if the URL contains "github.com" and either "blob" or "refs/tags"
        if (urlString.contains("github.com")) {
            // Replace "github.com" with "raw.githubusercontent.com"
            urlString = urlString.replace("github.com", "raw.githubusercontent.com");

            // Remove "/blob/" or "/refs/tags/" from the URL
            if (urlString.contains("/blob/")) {
                urlString = urlString.replace("/blob/", "/");
            } else if (urlString.contains("/refs/tags/")) {
                urlString = urlString.replace("/refs/tags/", "/");
            }

            // Return the modified URL as a URL object
            return new URL(urlString);
        } else {
            throw new IllegalArgumentException("Invalid GitHub URL format.");
        }
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
}
