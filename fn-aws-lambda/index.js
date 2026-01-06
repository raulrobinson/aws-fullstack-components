const fs = require("fs");
const path = require("path");

exports.handler = async (event) => {
    // Soporta dos formatos de entrada
    const username =
        event.queryStringParameters?.username ||
        event.username;

    if (!username) {
        return response(400, { message: "username is required" });
    }

    const filePath = path.join(process.cwd(), "users.json");
    const users = JSON.parse(fs.readFileSync(filePath, "utf-8"));

    const user = users.find(u => u.username === username);

    if (!user) {
        return response(404, { message: "User not found" });
    }

    return response(200, user);
};

const response = (statusCode, body) => ({
    statusCode,
    headers: {
        "Content-Type": "application/json"
    },
    body: JSON.stringify(body)
});
