package io.openliberty.mcp.internal.monitor;

public interface McpStatsMXBean {

	/*
	 * MCP Attributes
	 */
	public String getMcpMethodName();

	public String getErrorType();

	public String getGenAiPromptName();

	public String getGenAiToolName();

	public String getRpcResponseStatusCode() ;

	public String getGenAiOperationName() ;

	public String getJsonrpcProtocolVersion();

	public String getMcpProtocolVersion() ;

	public String getNetworkProtocolName() ;

	public String getNetworkProtocolVersion() ;

	public String getNetworkTransport() ;

	public String getMcpResourceUri() ;
	
	/*
	 * Metric values
	 */
	public long getCount();

	public double getDuration();
}
