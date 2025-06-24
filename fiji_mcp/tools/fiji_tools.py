import imagej

def get_java_dependencies():
    """
    Returns the jar files that need to be included into the classpath
    of an imagej object in order to have a functional ABBA app
    these jars should be available in https://maven.scijava.org/
    :return:
    """
    return ['net.imagej:imagej:2.16.0',
            'net.imagej:imagej-legacy:2.0.0',
            'ch.epfl.biop:fiji-tools:0.1.0-SNAPSHOT']


# globals.py or at the top of your module
_fiji_instance = None
_fiji_initialized = False

from jpype.types import JString

# Singleton Fiji instance
def get_tools_instance():
    global _fiji_tools, _fiji_initialized

    if not _fiji_initialized:
        print("Initializing Fiji instance...")
        ij = imagej.init(get_java_dependencies(), mode="interactive")
        ij.ui().showUI()
        from scyjava import jimport
        FijiTools = jimport('sc.fiji.tools.FijiTools')
        _fiji_tools = FijiTools(ij)
        _fiji_initialized = True
        print("Fiji instance initialized!")

    return _fiji_tools

from server import mcp

@mcp.tool()
def get_opened_images_information() -> str:
    """
        Returns the list of already opened images in Fiji and some of their properties (can be empty).

        If you need to act on one or several of these images, you can access them in a script with
            ```groovy
            String imageTitle = "example_title"
            ImagePlus image = ij.WindowManager.getImage(imageTitle)
            ```
        Alternatively, if you need to work on the active image, you can just get it with
            ```groovy
            #@ImagePlus image
            \\ do things with the image variable
            ```
        Args:
        Returns:
            a description of the images currently opened in Fiji as well as the code to retrieve them
    """
    return get_tools_instance().getCurrentState()

@mcp.tool()
def execute_groovy(groovy_code: str) -> str:
    """
        Execute a groovy script in the current opened Fiji instance
        Args:
            groovy_code: the groovy code to execute in the current Fiji instance. You can get some information about an object if you return it at the end of script. For instance
            you can return a String with some information that you want to gather from the script execution
        Returns:
            Returns a string representation of the returned object from the script if the execution is successful or an explanation of the error if one occurred. The structure is:
            class ExecutionResult:
                returnedObjectClass: str = None
                returnedObject: object = None
                executionSuccess: bool = False
                errorMessage: str = None
    """
    return str(get_tools_instance().executeGroovy(JString(groovy_code)))

@mcp.tool()
def show_or_update_script_in_editor(groovy_code: str, script_title: str) -> None:
    """
        Execute a groovy script in the current opened Fiji instance. You need to add the '.groovy' extension to the title for the language to be recognized by the editor
        Args:
            groovy_code: the groovy code to add in the editor - the user can thus see it and edit it if needed
            script_title: the title of the script. If a script with the same name exists, it will be replaced by this new one.
                This provides a way to update the script in the editor.
        Returns:
            Nothing
    """
    get_tools_instance().showOrUpdateScriptInEditor(JString(groovy_code), JString(script_title))

@mcp.tool()
def getScriptFromEditor(script_title: str) -> str:
    """
        Gets a groovy script from the Editor of the Fiji instance. The editor can contain different scripts with different names
        Args:
            script_title: the title of the script you want to fetch from the editor
        Returns:
            The script code
    """
    return str(get_tools_instance().getScriptFromEditor(JString(script_title)))

