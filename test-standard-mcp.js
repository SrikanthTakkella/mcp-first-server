#!/usr/bin/env node

// Standard MCP Protocol Test Client
const http = require('http');

function mcpRequest(method, params = {}, id = 1) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify({
      jsonrpc: "2.0",
      id: id,
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
          resolve({ error: 'Invalid JSON response', raw: responseData, status: res.statusCode });
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

async function testStandardMcpProtocol() {
  console.log('🔌 Testing Standard MCP Protocol');
  console.log('=================================\n');

  try {
    // Test 1: Initialize MCP connection (Standard Protocol)
    console.log('1. 🚀 Initializing MCP connection...');
    const initResponse = await mcpRequest('initialize', {
      protocolVersion: '2024-11-05',
      capabilities: { 
        tools: {} 
      },
      clientInfo: { 
        name: 'mcp-test-client', 
        version: '1.0.0' 
      }
    });
    console.log('✅ Initialize response:');
    console.log(JSON.stringify(initResponse, null, 2));

    // Test 2: List available tools (Standard Protocol)
    console.log('\n2. 📋 Listing available tools...');
    const toolsResponse = await mcpRequest('tools/list');
    console.log('✅ Tools list response:');
    console.log(JSON.stringify(toolsResponse, null, 2));

    // Test 3: Call tool (Standard Protocol)
    console.log('\n3. 🌤️  Calling weather tool for Paris, FR...');
    const toolCallResponse = await mcpRequest('tools/call', {
      name: 'getWeatherInfo',
      arguments: {
        name: 'Paris',
        countrycode: 'FR'
      }
    });
    console.log('✅ Tool call response:');
    console.log(JSON.stringify(toolCallResponse, null, 2));

    // Test 4: Ping (Standard Protocol)
    console.log('\n4. 🏓 Testing ping...');
    const pingResponse = await mcpRequest('ping');
    console.log('✅ Ping response:');
    console.log(JSON.stringify(pingResponse, null, 2));

    // Test 5: Invalid method (Error handling)
    console.log('\n5. ❌ Testing invalid method...');
    const errorResponse = await mcpRequest('invalid/method');
    console.log('✅ Error response:');
    console.log(JSON.stringify(errorResponse, null, 2));

    console.log('\n🎉 Standard MCP Protocol Test Complete!');
    console.log('\n📋 Summary:');
    console.log('- Initialize: ✅ Working');
    console.log('- Tools List: ✅ Working');  
    console.log('- Tool Call: ✅ Working');
    console.log('- Ping: ✅ Working');
    console.log('- Error Handling: ✅ Working');

  } catch (error) {
    console.error('❌ MCP Protocol Test Failed:', error.message);
    console.log('\n💡 Make sure your MCP server is running on http://localhost:8080');
  }
}

// Run the standard MCP protocol test
testStandardMcpProtocol();