const assert = require('assert');
const fs = require('fs');
const path = require('path');

const properties = fs.readFileSync(
    path.resolve(__dirname, '../../main/resources/application.properties'),
    'utf8'
);
const exampleProperties = fs.readFileSync(
    path.resolve(__dirname, '../../main/resources/application.properties.example'),
    'utf8'
);

for (const content of [properties, exampleProperties]) {
    assert(content.includes('spring.servlet.multipart.max-file-size=500MB'));
    assert(content.includes('spring.servlet.multipart.max-request-size=510MB'));
}

console.log('multipart upload limit checks passed');
