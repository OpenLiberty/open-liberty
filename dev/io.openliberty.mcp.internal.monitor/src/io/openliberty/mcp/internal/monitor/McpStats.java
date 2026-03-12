package io.openliberty.mcp.internal.monitor;

import com.ibm.websphere.monitor.meters.StatisticsMeter;
import com.ibm.websphere.monitor.meters.StatisticsReading;
import com.ibm.websphere.monitor.meters.Counter;
import com.ibm.websphere.monitor.meters.Meter;


public class McpStats extends Meter implements McpStatsMXBean {
	private final String mcpMethodName;
    
    /*
     * Conditionally required as per HTTP Semantics Convention
     */ 
    private final String errorType, genAiPromptName, genAiToolName, rpcResponseStatusCode;
    
    /*
     * Optional fields.
     * We are unable to facilitate capturing Exceptions
     * But we will leave it here.
     * Additional Context : We can capture  exceptions thrown by servlets
     * by surrounding the the chainFilter with try catch. But we have no way
     * of capturing application exception of Jaxrs/restfulws exceptions
     */
    private final String genAiOperationName, jsonrpcProtocolVersion, mcpProtocolVersion, networkProtocolName, networkProtocolVersion, networkTransport, mcpResourceUri;
	
	
	private Counter toolCallCount;
	private StatisticsMeter toolCallRunDuration;
	
	public McpStats(McpStatAttributes mcpStatAttributes) {
		this.mcpMethodName = mcpStatAttributes.getMcpMethodName();
		this.errorType = mcpStatAttributes.getErrorType();
		this.genAiPromptName = mcpStatAttributes.getGenAiPromptName();
		this.genAiToolName = mcpStatAttributes.getGenAiToolName();
		this.rpcResponseStatusCode = mcpStatAttributes.getRpcResponseStatusCode();
		this.genAiOperationName = mcpStatAttributes.getGenAiOperationName();
		this.jsonrpcProtocolVersion = mcpStatAttributes.getJsonrpcProtocolVersion();
		this.mcpProtocolVersion = mcpStatAttributes.getMcpMethodName();
		this.networkProtocolName = mcpStatAttributes.getNetworkProtocolName();
		this.networkProtocolVersion = mcpStatAttributes.getNetworkProtocolVersion();
		this.networkTransport = mcpStatAttributes.getNetworkTransport();
		this.mcpResourceUri = mcpStatAttributes.getMcpResourceUri();
		
		toolCallCount = new Counter();
		toolCallCount.setDescription("Total calls of an MCP tool");
		
		toolCallRunDuration = new StatisticsMeter();
		toolCallRunDuration.setDescription("Duration of tool call operations");
		toolCallRunDuration.setUnit("seconds");

	}

	public void incrementToolCallCountBy(int i) {
		toolCallCount.incrementBy(i);
	}
	
	public void addToolTimeStat(long time) {
		toolCallRunDuration.addDataPoint(time);
	}

	@Override
	public String getMcpMethodName() {
		return mcpMethodName;
	}

	@Override
	public String getErrorType() {
		return errorType;
	}

	@Override
	public String getGenAiPromptName() {
		return genAiPromptName;
	}

	@Override
	public String getGenAiToolName() {
		return genAiToolName;
	}

	@Override
	public String getRpcResponseStatusCode() {
		return rpcResponseStatusCode;
	}

	@Override
	public String getGenAiOperationName() {
		return genAiOperationName;
	}

	@Override
	public String getJsonrpcProtocolVersion() {
		return jsonrpcProtocolVersion;
	}

	@Override
	public String getMcpProtocolVersion() {
		return mcpProtocolVersion;
	}

	@Override
	public String getNetworkProtocolName() {
		return networkProtocolName;
	}

	@Override
	public String getNetworkProtocolVersion() {
		return networkProtocolVersion;
	}

	@Override
	public String getNetworkTransport() {
		return networkTransport;
	}

	@Override
	public String getMcpResourceUri() {
		return mcpResourceUri;
	}

	@Override
	public long getCount() {
		return toolCallCount.getCurrentValue();
	}

	@Override
	public double getDuration() {
		return toolCallRunDuration.getTotal();
	}
	
	




}
