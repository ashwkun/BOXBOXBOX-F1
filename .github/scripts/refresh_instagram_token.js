const https = require('https');
const { exec } = require('child_process');

const currentToken = process.env.IG_ACCESS_TOKEN;

if (!currentToken) {
    console.error('Error: IG_ACCESS_TOKEN env variable is missing');
    process.exit(1);
}

const url = `https://graph.instagram.com/refresh_access_token?grant_type=ig_refresh_token&access_token=${currentToken}`;

console.log('Refreshing Instagram Access Token...');

https.get(url, (res) => {
    let data = '';

    res.on('data', (chunk) => {
        data += chunk;
    });

    res.on('end', () => {
        try {
            if (res.statusCode !== 200) {
                throw new Error(`API returned status code ${res.statusCode}: ${data}`);
            }

            const response = JSON.parse(data);

            if (response.error) {
                throw new Error(`API Error: ${response.error.message}`);
            }

            const newToken = response.access_token;
            const expiresIn = response.expires_in;

            if (!newToken) {
                throw new Error('No access_token found in response');
            }

            console.log(`Successfully retrieved new token. Expires in: ${expiresIn} seconds (${(expiresIn / 86400).toFixed(1)} days)`);

            // Update GitHub Secret using gh CLI
            // Note: The workflow must be authenticated with a PAT for this to work
            console.log('Updating GitHub secret IG_ACCESS_TOKEN...');

            const updateCmd = `gh secret set IG_ACCESS_TOKEN --body "${newToken}"`;

            exec(updateCmd, (error, stdout, stderr) => {
                if (error) {
                    console.error(`Error updating secret: ${error.message}`);
                    console.error(`Command stderr: ${stderr}`);
                    process.exit(1);
                }
                console.log('✅ Successfully updated IG_ACCESS_TOKEN secret in GitHub.');
            });

        } catch (error) {
            console.error('Failed to refresh token:', error.message);
            process.exit(1);
        }
    });

}).on('error', (err) => {
    console.error('Network error calling Instagram API:', err.message);
    process.exit(1);
});
