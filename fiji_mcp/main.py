# main.py
from server import mcp


# Import tools so they get registered via decorators
import tools.fiji_tools

# Entry point to run the server
if __name__ == "__main__":
    import os

    os.environ['JAVA_HOME'] = r'C:\Users\Nicolas\.jdks\corretto-11.0.24'
    os.environ['PATH'] = r'C:\Users\Nicolas\.jdks\corretto-11.0.24\bin;' + os.environ.get('PATH', '')


    from tools.fiji_tools import get_tools_instance
    get_tools_instance() # Forces the initialisation of Fiji

    mcp.run()
