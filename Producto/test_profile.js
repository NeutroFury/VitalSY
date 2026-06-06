const http = require('http');

const data = JSON.stringify({
  email: 'test_basal_bolus2@test.com',
  password: 'password123',
  nombre: 'Test Basal Bolus User'
});

const req = http.request('http://localhost:8080/api/v1/auth/register', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': data.length
  }
}, (res) => {
  let body = '';
  res.on('data', chunk => body += chunk);
  res.on('end', () => {
    console.log("REGISTRO:", body);
    
    // Login
    const loginData = JSON.stringify({
      email: 'test_basal_bolus2@test.com',
      password: 'password123'
    });
    
    const loginReq = http.request('http://localhost:8080/api/v1/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': loginData.length
      }
    }, (lres) => {
      let lbody = '';
      lres.on('data', chunk => lbody += chunk);
      lres.on('end', () => {
        console.log("LOGIN:", lbody);
        const ljson = JSON.parse(lbody);
        const token = ljson.token;
        if(!token) return;
        
        // GET Profile
        const getReq = http.request('http://localhost:8080/api/v1/usuarios/perfil', {
          method: 'GET',
          headers: { 'Authorization': `Bearer ${token}` }
        }, (res2) => {
          let body2 = '';
          res2.on('data', chunk => body2 += chunk);
          res2.on('end', () => {
            console.log("GET PROFILE:", body2);
            
            // PUT Profile
            const putData = JSON.stringify({
              insulinaLenta: 'Tresiba (Degludec)',
              insulinaRapida: 'Humalog (Lispro)',
              pesoActual: 70,
              altura: 175,
              ratioIc: 10,
              factorIs: 40
            });
            
            const putReq = http.request('http://localhost:8080/api/v1/usuarios/perfil', {
              method: 'PUT',
              headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json',
                'Content-Length': putData.length
              }
            }, (res3) => {
              let body3 = '';
              res3.on('data', chunk => body3 += chunk);
              res3.on('end', () => {
                console.log("PUT PROFILE:", body3);
                
                // GET Profile 2
                const getReq2 = http.request('http://localhost:8080/api/v1/usuarios/perfil', {
                  method: 'GET',
                  headers: { 'Authorization': `Bearer ${token}` }
                }, (res4) => {
                  let body4 = '';
                  res4.on('data', chunk => body4 += chunk);
                  res4.on('end', () => {
                    console.log("GET PROFILE 2:", body4);
                  });
                });
                getReq2.end();
              });
            });
            putReq.write(putData);
            putReq.end();
          });
        });
        getReq.end();
      });
    });
    loginReq.write(loginData);
    loginReq.end();
  });
});
req.write(data);
req.end();
