#!/usr/bin/env node

// Simple MCP Client to test the weather tool
const http = require('http');

function mcpRequest(method, params = {}) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify({
      jsonrpc: "2.0",
      id: 1,
      method: method,
      params: params
    });

    const options = {
      hostname: 'localhost',
      port: 8080,
      path: '/mcp',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': data.length
      }
    };

    const req = http.request(options, (res) => {
      let responseData = '';
      
      res.on('data', (chunk) => {
        responseData += chunk;
      });
      
      res.on('end', () => {
        try {
          const result = JSON.parse(responseData);
          resolve(result);
        } catch (e) {
          resolve({ error: 'Invalid JSON response', raw: responseData });
        }
      });
    });

    req.on('error', (err) => {
      reject(err);
    });

    req.write(data);
    req.end();
  });
}

async function testMcpServer() {
  console.log('🌤️  Testing MCP Weather Server');
  console.log('===============================\n');

  try {
    // Test 1: Initialize connection
    console.log('1. Initializing MCP connection...');
    const initResponse = await mcpRequest('initialize', {
      protocolVersion: '2024-11-05',
      capabilities: { tools: {} },
      clientInfo: { name: 'weather-test-client', version: '1.0.0' }
    });
    console.log('✅ Init response:', JSON.stringify(initResponse, null, 2));

    // Test 2: List available tools
    console.log('\n2. Listing available tools...');
    const toolsResponse = await mcpRequest('tools/list');
    console.log('✅ Tools response:', JSON.stringify(toolsResponse, null, 2));

    // Test 3: Call the weather tool
    console.log('\n3. Testing weather tool for Vancouver, CA...');
    const weatherResponse = await mcpRequest('tools/call', {
      name: 'getWeatherInfo',
      arguments: {
        name: 'Vancouver',
        countrycode: 'CA'
      }
    });
    console.log('✅ Weather response:', JSON.stringify(weatherResponse, null, 2));

    // Test 4: Test another city
    console.log('\n4. Testing weather tool for London, GB...');
    const weather2Response = await mcpRequest('tools/call', {
      name: 'getWeatherInfo',
      arguments: {
        name: 'London',
        countrycode: 'GB'
      }
    });
    console.log('✅ Weather response:', JSON.stringify(weather2Response, null, 2));

  } catch (error) {
    console.error('❌ Error testing MCP server:', error.message);
  }
}

// Run the test
testMcpServer();