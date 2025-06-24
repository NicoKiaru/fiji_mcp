# Proof of concept - MCP server for ImageJ / Fiji

This repo contains a basic MCP server for ImageJ/Fiji. It consists of 2 repositories:

* **fiji-tools**: (Java) contains a few functions that facilitates the comminucation with a LLM. The GUI for coding (editor) is easily accessible to a user, but not to a bot
* **fiji_mcp**: (Python) simple wrapper of Fiji functions, exposed as tools for the MCP server

To get this server working with Claude Desktop, you can add this in the configuration:

```json
{"mcpServers": 
	{
		"fiji_mcp": {
			"command": "uv",
			"args": [
				"--directory",
				"C:/Users/Nicolas/PycharmProjects/fiji_mcp",
				"run",
				"main.py"
			]
		}
	}
}
```

The jar from fiji-tools has to be built - it's only available as SNAPSHOT at the moment. 

```
	<groupId>ch.epfl.biop</groupId>
	<artifactId>fiji-tools</artifactId>
	<version>0.1.0-SNAPSHOT</version>
```

Note that the jvm path has to be hardcoded in the Python `main.py` code:

```python
# Entry point to run the server
if __name__ == "__main__":
    import os

    os.environ['JAVA_HOME'] = r'C:\Users\Nicolas\.jdks\corretto-11.0.24'
    os.environ['PATH'] = r'C:\Users\Nicolas\.jdks\corretto-11.0.24\bin;' + os.environ.get('PATH', '')
```

Otherwise in the context where Claude Desktop is executed, the jvm cannot be found.