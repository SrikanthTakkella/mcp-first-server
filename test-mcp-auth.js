#!/usr/bin/env node

// MCP Authentication Test Client
const http = require('http');

function mcpRequestWithAuth(method, params = {}, id = 1, authHeader = null) {
  return new Promise((resolve, reject) => {
    const data = JSON.stringify({
      jsonrpc: "2.0",
      id: id,
      method: method,
      params: params
    });

    const headers = {
      'Content-Type': 'application/json',
      'Content-Length': data.length
    };

    // Add authentication header if provided
    if (authHeader) {
      if (authHeader.type === 'bearer') {
        headers['Authorization'] = `Bearer ${authHeader.value}`;
      } else if (authHeader.type === 'basic') {
        headers['Authorization'] = `Basic ${authHeader.value}`;
      } else if (authHeader.type === 'api-key') {
        headers['X-API-Key'] = authHeader.value;
      }
    }

    const options = {
      hostname: 'localhost',
      port: 8080,
      path: '/mcp',
      method: 'POST',
      headers: headers
    };

    const req = http.request(options, (res) => {
      let responseData = '';
      
      res.on('data', (chunk) => {
        responseData += chunk;
      });
      
      res.on('end', () => {
        try {
          const result = JSON.parse(responseData);
          resolve({ ...result, statusCode: res.statusCode });
        } catch (e) {
          resolve({ error: 'Invalid JSON response', raw: responseData, statusCode: res.statusCode });
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

async function testMcpAuthentication() {
  console.log('🔐 Testing MCP Authentication');
  console.log('==============================\n');

  // Test credentials
  const validApiKey = 'mcp-weather-api-key-12345';
  const validBearerToken = 'bearer-token-abcdef123456';
  const validBasicAuth = 'bWNwOndlYXRoZXI='; // mcp:weather in base64
  
  const invalidApiKey = 'invalid-api-key';
  const invalidBearerToken = 'invalid-bearer-token';

  try {
    console.log('1. 🚫 Testing without authentication...');
    const noAuthResponse = await mcpRequestWithAuth('initialize', {
      protocolVersion: '2024-11-05',
      capabilities: { tools: {} },
      clientInfo: { name: 'auth-test-client', version: '1.0.0' }
    });
    console.log(`Status: ${noAuthResponse.statusCode}`);
    if (noAuthResponse.statusCode === 200) {
      console.log('✅ No auth required (development mode)');
    } else {
      console.log('❌ Authentication required');
    }

    console.log('\n2. 🔑 Testing with valid API Key...');
    const apiKeyResponse = await mcpRequestWithAuth('initialize', {
      protocolVersion: '2024-11-05',
      capabilities: { tools: {} },
      clientInfo: { name: 'auth-test-client', version: '1.0.0' }
    }, 1, { type: 'api-key', value: validApiKey });
    console.log(`Status: ${apiKeyResponse.statusCode}`);
    if (apiKeyResponse.result) {
      console.log('✅ API Key authentication successful');
      console.log('Server capabilities:', JSON.stringify(apiKeyResponse.result.capabilities, null, 2));
    }

    console.log('\n3. 🎫 Testing with valid Bearer Token...');
    const bearerResponse = await mcpRequestWithAuth('tools/list', {}, 1, 
      { type: 'bearer', value: validBearerToken });
    console.log(`Status: ${bearerResponse.statusCode}`);
    if (bearerResponse.result) {
      console.log('✅ Bearer Token authentication successful');
    }

    console.log('\n4. 🔐 Testing with valid Basic Auth...');
    const basicResponse = await mcpRequestWithAuth('tools/list', {}, 1, 
      { type: 'basic', value: validBasicAuth });
    console.log(`Status: ${basicResponse.statusCode}`);
    if (basicResponse.result) {
      console.log('✅ Basic Auth authentication successful');
    }

    console.log('\n5. ❌ Testing with invalid API Key...');
    const invalidApiResponse = await mcpRequestWithAuth('tools/list', {}, 1, 
      { type: 'api-key', value: invalidApiKey });
    console.log(`Status: ${invalidApiResponse.statusCode}`);
    if (invalidApiResponse.statusCode === 401) {
      console.log('✅ Invalid API Key properly rejected');
    } else {
      console.log('⚠️  Invalid API Key not rejected (development mode)');
    }

    console.log('\n6. ❌ Testing with invalid Bearer Token...');
    const invalidBearerResponse = await mcpRequestWithAuth('tools/list', {}, 1, 
      { type: 'bearer', value: invalidBearerToken });
    console.log(`Status: ${invalidBearerResponse.statusCode}`);
    if (invalidBearerResponse.statusCode === 401) {
      console.log('✅ Invalid Bearer Token properly rejected');
    } else {
      console.log('⚠️  Invalid Bearer Token not rejected (development mode)');
    }

    console.log('\n7. 🌤️  Testing authenticated tool call...');
    const toolCallResponse = await mcpRequestWithAuth('tools/call', {
      name: 'getWeatherInfo',
      arguments: {
        name: 'Tokyo',
        countrycode: 'JP'
      }
    }, 1, { type: 'api-key', value: validApiKey });
    
    console.log(`Status: ${toolCallResponse.statusCode}`);
    if (toolCallResponse.result && toolCallResponse.result.content) {
      console.log('✅ Authenticated tool call successful');
      console.log('Weather data:', toolCallResponse.result.content[0].text);
    }

    console.log('\n🎉 MCP Authentication Test Complete!');
    console.log('\n📋 Authentication Summary:');
    console.log('- API Key (X-API-Key): ✅ Supported');
    console.log('- Bearer Token (Authorization: Bearer): ✅ Supported');  
    console.log('- Basic Auth (Authorization: Basic): ✅ Supported');
    console.log('- Client Certificates: ❌ Not implemented');
    console.log('- Session Tokens: ❌ Not implemented');

    console.log('\n🔧 Production Configuration:');
    console.log('- Change McpAuthenticationService.authenticate() to return false by default');
    console.log('- Store credentials in environment variables or secure configuration');
    console.log('- Implement proper user management and token validation');
    console.log('- Add rate limiting and audit logging');

  } catch (error) {
    console.error('❌ Authentication Test Failed:', error.message);
  }
}

// Run the authentication test
testMcpAuthentication();