// PM_QBM_029
// Test lấy danh sách question type thành công
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const body = pm.response.json();

// Kiểm tra data là 1 danh sách
pm.test("Data is an array", function () {
    pm.expect(Array.isArray(body)).to.eql(true);
});

// Kiểm tra mỗi question type có các field cơ bản
pm.test("Each question type has basic fields", function () {
    body.forEach((qt, idx) => {
        pm.expect(qt, `questionType[${idx}]`).to.have.property("id");
        pm.expect(qt, `questionType[${idx}]`).to.have.property("typeCode");
        pm.expect(qt, `questionType[${idx}]`).to.have.property("description");
    });
});

//--------------------------------------------------------------------------- 
// PM_QBM_030
// Test lấy question type theo id thành công (GET /api/question-types/id/{id})
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const bodyById = pm.response.json();
const pathSegmentsId = pm.request.url.getPath().split("/").filter(Boolean);
const pathId = Number(pathSegmentsId[pathSegmentsId.length - 1]);

// Kiểm tra object trả về có các field cơ bản
pm.test("QuestionType by id has basic fields", function () {
    pm.expect(bodyById).to.have.property("id");
    pm.expect(bodyById).to.have.property("typeCode");
    pm.expect(bodyById).to.have.property("description");
});

// Kiểm tra id khớp với id trên URL
pm.test("Id matches path id", function () {
    pm.expect(bodyById.id).to.eql(pathId);
});


//---------------------------------------------------------------------------
// PM_QBM_031
// Test lấy question type theo id không tồn tại
// Kiểm tra status code
pm.test("Status code is 404", function () {
    pm.response.to.have.status(404);
});

const body = pm.response.json();
// Kiểm tra message
pm.test("Message is Question type is not found", function () {
    pm.expect(body.message).to.eql("Question type is not found");
});


//--------------------------------------------------------------------------- 
// PM_QT_0032
// Test lấy question type theo typeCode thành công
// Kiểm tra status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

const bodyByTypeCode = pm.response.json();
const pathSegmentsCode = pm.request.url.getPath().split("/").filter(Boolean);
const pathTypeCode = pathSegmentsCode[pathSegmentsCode.length - 1];

// Kiểm tra object trả về có các field cơ bản
pm.test("QuestionType by typeCode has basic fields", function () {
    pm.expect(bodyByTypeCode).to.have.property("id");
    pm.expect(bodyByTypeCode).to.have.property("typeCode");
    pm.expect(bodyByTypeCode).to.have.property("description");
});

// Kiểm tra typeCode khớp URL
pm.test("typeCode matches path typeCode", function () {
    pm.expect(String(bodyByTypeCode.typeCode).toUpperCase()).to.eql(String(pathTypeCode).toUpperCase());
});


//---------------------------------------------------------------------------
// PM_QBM_033
// Test lấy question type theo id không tồn tại
// Kiểm tra status code
pm.test("Status code is 404", function () {
    pm.response.to.have.status(404);
});

const body = pm.response.json();
// Kiểm tra message
pm.test("Message is Code type is not found", function () {
    pm.expect(body.message).to.eql("Code type is not found");
});