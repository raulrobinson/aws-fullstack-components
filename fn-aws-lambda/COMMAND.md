- Create a role
```text
aws iam create-role \
  --role-name lambda-user-search-role \
  --endpoint-url=http://localhost:4566 \
  --assume-role-policy-document file://trust-policy.json
```

- Attach the AWSLambdaBasicExecutionRole policy to the role
```text
aws iam attach-role-policy \
  --role-name lambda-user-search-role \
  --endpoint-url=http://localhost:4566 \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
```

- Package the Lambda function code and dependencies into a ZIP file
```text
Compress-Archive -Path index.js, users.json -DestinationPath function.zip -Force
```

- Create the Lambda function
```text
aws lambda create-function \
  --function-name user-search-lambda \
  --runtime nodejs20.x \
  --role arn:aws:iam::000000000000:role/lambda-user-search-role \
  --handler index.handler \
  --zip-file fileb://function.zip \
  --timeout 10 \
  --endpoint-url=http://localhost:4566 \
  --memory-size 128
```

- Invoke the Lambda function
```text
aws lambda invoke \
  --function-name user-search-lambda \
  --endpoint-url=http://localhost:4566 \
  --cli-binary-format raw-in-base64-out \
  --payload file://payload.json \
  response.json
```

- Update the Lambda function code
```text
aws lambda update-function-code \
  --function-name user-search-lambda \
  --zip-file fileb://function.zip \
  --endpoint-url=http://localhost:4566
```

- Delete the Lambda function
```text
aws lambda delete-function \
  --function-name user-search-lambda \
  --endpoint-url=http://localhost:4566
```