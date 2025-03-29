// send-sos.js

const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const twilio = require('twilio');
const fs = require('fs');
const path = require('path');

const accountSid = 'ACe1d60fcb87b42a6c3706e0d25d8f6b41';
const authToken = 'ef1748398913c06100c68d44cfc46ccf';
const client = twilio(accountSid, authToken);

const app = express();
const PORT = 3000;

app.use(cors());
app.use(bodyParser.json());

app.post('/sos', async (req, res) => {
    const { name, surname, coordinates, contacts, callMeAt, emergencyType } = req.body;

  if (!name || !surname) {
    return res.status(400).json({ error: 'Name and surname are required' });
  }

  if (!contacts || !Array.isArray(contacts) || contacts.length === 0) {
    return res.status(400).json({ error: 'Contacts array is required' });
  }

  if (!coordinates || typeof coordinates !== 'string') {
    return res.status(400).json({ error: 'Coordinates string is required' });
  }

  if (!callMeAt || typeof callMeAt !== 'string') {
    return res.status(400).json({ error: 'callMeAt is required as a string' });
  }
  
  if (!emergencyType || typeof emergencyType !== 'string') {
    return res.status(400).json({ error: 'emergencyType is required as a string' });
  }  

  const messageBody = `🚨 SOS ALERT 🚨
  Name: ${name} ${surname}
  Emergency: ${emergencyType}
  Location: https://maps.google.com/?q=${coordinates}
  📞 Call me at: ${callMeAt}`;
  

  const results = [];

  for (const number of contacts) {
    try {
      const message = await client.messages.create({
        from: 'whatsapp:+14155238886',
        to: `whatsapp:${number}`,
        body: messageBody
      });

      results.push({ number, sid: message.sid, status: 'sent' });
    } catch (err) {
      results.push({ number, error: err.message, status: 'failed' });
    }
  }

  // ✅ Save to logs.json
  const logEntry = {
    timestamp: new Date().toISOString(),
    name,
    surname,
    coordinates,
    contacts,
    results
  };

  const logPath = path.join(__dirname, 'logs.json');
  let logs = [];

  if (fs.existsSync(logPath)) {
    const file = fs.readFileSync(logPath, 'utf-8');
    logs = JSON.parse(file);
  }

  logs.push(logEntry);
  fs.writeFileSync(logPath, JSON.stringify(logs, null, 2));

  res.status(200).json({ message: 'SOS sent successfully', results });
});

// Optional GET / for browser testing
app.get('/', (req, res) => {
  res.send('✅ SOS API is running. Use POST /sos to send alerts.');
});

app.listen(PORT, () => {
  console.log(`🚀 SOS API running on http://localhost:${PORT}`);
});
