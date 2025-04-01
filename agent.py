import asyncio
from mcp_agent.core.fastagent import FastAgent

# Create the application
fast = FastAgent("FastAgent Example")

# Define the agent
@fast.agent(
    instruction="You are a helpful AI Agent that is able to look up locations in the " +
                "United States by fetching latitude and longitude coordinates for " +
                "locations and using that to call the weather tool to answer questions " +
                "about the weather of various locations. When fetching latitude and " +
                "longitude for a location, try wikipedia first. If you're asked for a location" +
                "that is not in the United States, you should respond with a message saying " +
                "that the location is outside the United States and you can't help with " +
                "that. Always answer as concisely as possible, if you can answer the question" +
                "in a single sentence, do so.",
    servers=[
        "fetch",
        "weather"
    ]
)
async def main():
    # use the --model command line switch or agent arguments to change model
    async with fast.run() as agent:
        await agent()


if __name__ == "__main__":
    asyncio.run(main())
