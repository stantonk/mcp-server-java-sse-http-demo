# Weather Java SSE Transport MCP Service

A working implementation Model Context Protocol java-sdk. Includes
a fast-agent for the MCP Client to interact with the Weather mcp-server.

⚠️This isn't remotely production grade, just demonstrating how to build
something working end to end that can leverage multiple MCP Servers both
local (Stdio Transport) and remote (HTTP SSE Transport). Also avoids
pulling in as much of Spring as possible, for those who don't want it.

In the demo, you'll see the agent:

1. The agent uses Claude Sonnet 3.7 throughout for the LLM bits
2. The agent calls out to the "fetch" websearch MCP Server tool to look up lat/long for each city I asked for.
3. The agent then uses those lat/long values to call the weather MCP Server tool (implemented in this repository).
You can see the calls being made in the upper half of the terminal split window.
4. The agent summarizes the forecasts and determine which of the 4 cities is the warmest tomorrow.

**The agent is figuring out which tools to call from the context of the user's inputted prompts.**

![Description of image](docs/mcp-server-weather-demo.gif)

* [Model Context Protocol](https://github.com/modelcontextprotocol)
* [java-sdk](https://github.com/modelcontextprotocol/java-sdk)
* [List of Model Context Protocol Servers](https://github.com/modelcontextprotocol/servers?tab=readme-ov-file#model-context-protocol-servers)
## build and start Weather MCP Server

```
mvn clean package
java -jar java -jar mcp-server/target/mcp-server-1.0-SNAPSHOT.jar
```

## install, configure, and start fast-agent

### install
```
pip install uv
uv pip install fast-agent-mcp
uv run agent.py
```

### Configure Claude and OpenAI api keys

`cp fastagent.secrets.yaml.TEMPLATE fastagent.secrets.yaml`

then add your api keys to that config file.

### Note: Weather MCP Server config

Note, in `fastagent.config.yaml` we've configured the MCP Servers available to the agent,
in this case our server uses the SSE HTTP Transport, this enables
calling remote MCP Servers :).

```
mcp:
    servers:
        weather:
            transport: "sse"
            read_timeout_seconds: 10
            url: "http://localhost:8080/sse"
```

## Lower level for testing

Connect to SSE stream
```
curl -v -H "Accept: text/event-stream" "http://localhost:8080/sse"
```

Send messages to /mcp/messages (note, include sessionId from SSE)
```
curl -X POST "http://localhost:8080/mcp/message?sessionId=b64ad193-6bb3-4e2e-b33c-27a23011acb4" -d '{"hi": "mynameis"}' | jq .
```

Things to check out:

* https://github.com/youngsu5582/mcp-server-mysql
