//PM_EM_001
//Kiểm tra lấy danh sách exam theo trang thành công
pm.test("Status 200", () => pm.response.to.have.status(200));

const contentType = pm.response.headers.get("Content-Type") || "";
pm.test("Content-Type là JSON", () => {
  pm.expect(contentType).to.include("application/json");
});

const body = pm.response.json();
pm.test("PageResult hợp lệ", () => {
  pm.expect(body).to.be.an("object");
  pm.expect(body).to.have.property("data");
  pm.expect(body.data).to.be.an("array");
});


