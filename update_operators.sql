-- ============================================
-- Update operator table with complete data
-- Maintaining: id, operator_id, organization_code, organization_name
-- Adding: age, gender, phone_number, address, address_detail, join_date
-- ============================================

-- Emergency Medical Technicians
UPDATE operator SET age = 32, gender = 'Male', phone_number = '010-1234-5601', address = '서울시 강남구', address_detail = '테헤란로 123', join_date = '2018-03-15'::timestamp WHERE id = 'op-05028b54-33d2-450e-bafe-b4f355fefacd';
UPDATE operator SET age = 29, gender = 'Male', phone_number = '010-1234-5602', address = '서울시 서초구', address_detail = '서초대로 456', join_date = '2019-06-20'::timestamp WHERE id = 'op-9a8833ad-c5cb-4cd0-9133-4dc177b91e9b';
UPDATE operator SET age = 35, gender = 'Female', phone_number = '010-1234-5603', address = '서울시 송파구', address_detail = '올림픽로 789', join_date = '2016-09-10'::timestamp WHERE id = 'op-7fcb4830-8626-48a8-be3d-51049351aebe';
UPDATE operator SET age = 31, gender = 'Male', phone_number = '010-1234-5604', address = '서울시 강서구', address_detail = '공항대로 321', join_date = '2017-11-25'::timestamp WHERE id = 'op-ede5a6c8-9adc-44ab-b0b2-2df4c643e4b8';
UPDATE operator SET age = 28, gender = 'Female', phone_number = '010-1234-5605', address = '서울시 마포구', address_detail = '월드컵로 654', join_date = '2020-02-14'::timestamp WHERE id = 'op-dff768ab-2699-4d68-b71a-b3d92fd02d8b';
UPDATE operator SET age = 33, gender = 'Male', phone_number = '010-1234-5606', address = '서울시 용산구', address_detail = '한강대로 987', join_date = '2018-07-08'::timestamp WHERE id = 'op-d0558094-1382-4fbf-90c4-00293d6b2abd';
UPDATE operator SET age = 30, gender = 'Female', phone_number = '010-1234-5607', address = '서울시 성동구', address_detail = '왕십리로 246', join_date = '2019-01-30'::timestamp WHERE id = 'op-62af6461-270c-42cd-b187-b9b5a26b556a';
UPDATE operator SET age = 27, gender = 'Male', phone_number = '010-1234-5608', address = '서울시 광진구', address_detail = '능동로 135', join_date = '2021-04-12'::timestamp WHERE id = 'op-c0f5a407-2627-45fd-94fe-5c5523af8ce9';
UPDATE operator SET age = 34, gender = 'Male', phone_number = '010-1234-5609', address = '서울시 은평구', address_detail = '진관로 753', join_date = '2017-08-22'::timestamp WHERE id = 'op-5c710c9a-4880-4078-a2ab-5ed85e0f71ec';
UPDATE operator SET age = 29, gender = 'Female', phone_number = '010-1234-5610', address = '서울시 노원구', address_detail = '상계로 951', join_date = '2020-05-17'::timestamp WHERE id = 'op-bf72a69c-1272-4deb-bd9e-80c58682b01a';

-- Rescue Technicians
UPDATE operator SET age = 36, gender = 'Male', phone_number = '010-2345-6701', address = '서울시 중구', address_detail = '을지로 111', join_date = '2015-04-10'::timestamp WHERE id = 'op-289c4a12-5bae-4fed-8047-3c0e6cc8e8fe';
UPDATE operator SET age = 38, gender = 'Male', phone_number = '010-2345-6702', address = '서울시 종로구', address_detail = '종로 222', join_date = '2014-07-18'::timestamp WHERE id = 'op-390e2350-5ed3-4d1c-bc19-bb607d875fe7';
UPDATE operator SET age = 33, gender = 'Male', phone_number = '010-2345-6703', address = '서울시 성북구', address_detail = '동소문로 333', join_date = '2018-02-05'::timestamp WHERE id = 'op-dd77ab44-5ab5-4e4a-b517-8048a510a5e6';
UPDATE operator SET age = 31, gender = 'Female', phone_number = '010-2345-6704', address = '서울시 동대문구', address_detail = '청량리로 444', join_date = '2019-09-12'::timestamp WHERE id = 'op-2484cd98-7026-44cd-965e-d7fc3b29fade';
UPDATE operator SET age = 35, gender = 'Male', phone_number = '010-2345-6705', address = '서울시 중랑구', address_detail = '망우로 555', join_date = '2016-12-20'::timestamp WHERE id = 'op-70725618-9916-48ae-a547-d9c7e901d7d9';
UPDATE operator SET age = 40, gender = 'Male', phone_number = '010-2345-6706', address = '서울시 강북구', address_detail = '도봉로 666', join_date = '2013-05-15'::timestamp WHERE id = 'op-b587d4b3-4aae-41f3-97a4-acd789490fbe';
UPDATE operator SET age = 32, gender = 'Male', phone_number = '010-2345-6707', address = '서울시 도봉구', address_detail = '방학로 777', join_date = '2018-10-08'::timestamp WHERE id = 'op-7796faa9-8bd3-4ac9-b217-6f7ce3efa3a6';
UPDATE operator SET age = 29, gender = 'Female', phone_number = '010-2345-6708', address = '서울시 관악구', address_detail = '신림로 888', join_date = '2020-03-25'::timestamp WHERE id = 'op-318a3e50-d23d-4375-80b8-18d753013ef5';
UPDATE operator SET age = 34, gender = 'Male', phone_number = '010-2345-6709', address = '서울시 서대문구', address_detail = '연희로 999', join_date = '2017-06-30'::timestamp WHERE id = 'op-f37385ca-c7c2-4234-b6ce-c87e5039b7bf';
UPDATE operator SET age = 37, gender = 'Male', phone_number = '010-2345-6710', address = '서울시 양천구', address_detail = '목동로 121', join_date = '2015-11-14'::timestamp WHERE id = 'op-bb65b4e5-e4f2-4e83-a314-9df15fd6fedd';

-- Firefighters
UPDATE operator SET age = 42, gender = 'Male', phone_number = '010-3456-7801', address = '서울시 영등포구', address_detail = '여의대로 100', join_date = '2012-03-20'::timestamp WHERE id = 'op-002c6ffd-b420-474b-b933-c928e1cc1812';
UPDATE operator SET age = 39, gender = 'Female', phone_number = '010-3456-7802', address = '서울시 동작구', address_detail = '사당로 200', join_date = '2014-08-15'::timestamp WHERE id = 'op-1a03c114-8393-40c1-8c9f-dad22f0f5a3b';
UPDATE operator SET age = 35, gender = 'Male', phone_number = '010-3456-7803', address = '서울시 구로구', address_detail = '디지털로 300', join_date = '2016-01-10'::timestamp WHERE id = 'op-fb79c268-32b0-4f44-affb-fbebfdf6014f';
UPDATE operator SET age = 31, gender = 'Male', phone_number = '010-3456-7804', address = '서울시 금천구', address_detail = '가산디지털로 400', join_date = '2019-05-22'::timestamp WHERE id = 'op-2b55159c-75a2-49d5-b58f-24b8ae0851e3';
UPDATE operator SET age = 28, gender = 'Female', phone_number = '010-3456-7805', address = '서울시 강동구', address_detail = '천호대로 500', join_date = '2021-02-18'::timestamp WHERE id = 'op-fe8cb340-41a6-4592-bbc6-4bf6b860ff9a';
UPDATE operator SET age = 30, gender = 'Male', phone_number = '010-3456-7806', address = '서울시 송파구', address_detail = '송파대로 600', join_date = '2020-07-09'::timestamp WHERE id = 'op-f5cfc6ea-6952-4b49-8003-ed0db97dacc5';
UPDATE operator SET age = 33, gender = 'Female', phone_number = '010-3456-7807', address = '서울시 강남구', address_detail = '강남대로 700', join_date = '2018-11-30'::timestamp WHERE id = 'op-b6325664-e1b5-4cf6-9746-722984b210ff';
UPDATE operator SET age = 41, gender = 'Male', phone_number = '010-3456-7808', address = '서울시 서초구', address_detail = '반포대로 800', join_date = '2013-09-05'::timestamp WHERE id = 'op-cdef5c7c-f70d-4803-8860-29c5d9901194';
UPDATE operator SET age = 36, gender = 'Female', phone_number = '010-3456-7809', address = '서울시 광진구', address_detail = '광나루로 900', join_date = '2016-04-28'::timestamp WHERE id = 'op-a52af4bf-b547-4a71-803c-63100c9dbca8';
UPDATE operator SET age = 34, gender = 'Male', phone_number = '010-3456-7810', address = '서울시 성동구', address_detail = '성수이로 1000', join_date = '2017-12-14'::timestamp WHERE id = 'op-6c788fee-581c-4974-aae5-a30488c810b3';

-- Police Officers
UPDATE operator SET age = 37, gender = 'Male', phone_number = '010-4567-8901', address = '서울시 용산구', address_detail = '이태원로 111', join_date = '2015-06-12'::timestamp WHERE id = 'op-126fb142-b6d5-4d21-ac16-ad28eeccf1de';
UPDATE operator SET age = 40, gender = 'Male', phone_number = '010-4567-8902', address = '서울시 마포구', address_detail = '마포대로 222', join_date = '2013-10-25'::timestamp WHERE id = 'op-e0d278a8-f9ed-417e-9bb7-8cc90cf2ac0a';
UPDATE operator SET age = 45, gender = 'Male', phone_number = '010-4567-8903', address = '서울시 중구', address_detail = '명동길 333', join_date = '2010-02-15'::timestamp WHERE id = 'op-20b3ac74-2766-4e19-bff8-723dc3bae2a0';
UPDATE operator SET age = 35, gender = 'Male', phone_number = '010-4567-8904', address = '서울시 종로구', address_detail = '인사동길 444', join_date = '2016-08-20'::timestamp WHERE id = 'op-a29bd99b-7d68-4805-92ca-4c21aec9c921';
UPDATE operator SET age = 32, gender = 'Female', phone_number = '010-4567-8905', address = '서울시 강남구', address_detail = '논현로 555', join_date = '2019-03-18'::timestamp WHERE id = 'op-9005ea4a-604a-4177-9e92-3fd79b1bdbda';
UPDATE operator SET age = 29, gender = 'Male', phone_number = '010-4567-8906', address = '서울시 서초구', address_detail = '강남대로 666', join_date = '2021-01-10'::timestamp WHERE id = 'op-84e6fd4c-0a5f-4a02-8eb2-a8b98ff86b46';
UPDATE operator SET age = 33, gender = 'Male', phone_number = '010-4567-8907', address = '서울시 송파구', address_detail = '잠실로 777', join_date = '2018-05-22'::timestamp WHERE id = 'op-b9743b43-a15e-422e-8f30-e181e571ecc8';
UPDATE operator SET age = 38, gender = 'Male', phone_number = '010-4567-8908', address = '서울시 강동구', address_detail = '올림픽로 888', join_date = '2014-11-08'::timestamp WHERE id = 'op-d9e7bfcf-6bb6-463d-a9a1-796d6109c603';
UPDATE operator SET age = 31, gender = 'Female', phone_number = '010-4567-8909', address = '서울시 광진구', address_detail = '자양로 999', join_date = '2019-07-14'::timestamp WHERE id = 'op-838cf2c4-d3cc-4065-b701-395cbabb1313';
UPDATE operator SET age = 28, gender = 'Male', phone_number = '010-4567-8910', address = '서울시 성동구', address_detail = '뚝섬로 1010', join_date = '2020-09-30'::timestamp WHERE id = 'op-8b7d5578-54a8-4487-ba45-2a192fcb748e';

-- Operators (Intake Desk)
UPDATE operator SET age = 26, gender = 'Male', phone_number = '010-5678-9001', address = '서울시 강남구', address_detail = '역삼로 100', join_date = '2022-01-15'::timestamp WHERE id = 'op-1ef8d533-f18f-4f23-8331-7f7cfc7f7552';
UPDATE operator SET age = 27, gender = 'Female', phone_number = '010-5678-9002', address = '서울시 서초구', address_detail = '방배로 200', join_date = '2021-11-20'::timestamp WHERE id = 'op-6a8d018a-b224-44d9-bfd3-9d26263a3455';
UPDATE operator SET age = 25, gender = 'Male', phone_number = '010-5678-9003', address = '서울시 송파구', address_detail = '문정로 300', join_date = '2022-06-10'::timestamp WHERE id = 'op-7b59547e-eefe-4488-b56d-59d87712d8b9';
UPDATE operator SET age = 28, gender = 'Female', phone_number = '010-5678-9004', address = '서울시 강동구', address_detail = '명일로 400', join_date = '2021-03-25'::timestamp WHERE id = 'op-000e843b-9902-4e6d-8ba0-46caaef9041b';
UPDATE operator SET age = 29, gender = 'Male', phone_number = '010-5678-9005', address = '서울시 광진구', address_detail = '구의로 500', join_date = '2020-12-08'::timestamp WHERE id = 'op-b8e20ae8-7565-4abf-8460-eb4f73f566b3';
UPDATE operator SET age = 24, gender = 'Female', phone_number = '010-5678-9006', address = '서울시 성동구', address_detail = '왕십리로 600', join_date = '2023-02-14'::timestamp WHERE id = 'op-66538b2f-69dc-43e0-ac40-cdfcd2545cb7';
UPDATE operator SET age = 26, gender = 'Male', phone_number = '010-5678-9007', address = '서울시 중랑구', address_detail = '면목로 700', join_date = '2022-08-22'::timestamp WHERE id = 'op-2ac78654-de95-4dde-a8da-70faceb377fe';
UPDATE operator SET age = 27, gender = 'Female', phone_number = '010-5678-9008', address = '서울시 동대문구', address_detail = '천호대로 800', join_date = '2021-09-18'::timestamp WHERE id = 'op-87cf89a7-8fa3-48ea-a217-05942cbddc67';
UPDATE operator SET age = 30, gender = 'Male', phone_number = '010-5678-9009', address = '서울시 노원구', address_detail = '노원로 900', join_date = '2020-04-30'::timestamp WHERE id = 'op-fd805d2b-30df-4ef6-80b5-2d0f0fed05d1';
UPDATE operator SET age = 25, gender = 'Female', phone_number = '010-5678-9010', address = '서울시 도봉구', address_detail = '도봉로 1000', join_date = '2022-10-12'::timestamp WHERE id = 'op-fc51a0f4-bf45-4659-b43f-53f30531e1c2';

-- ============================================
-- Total: 49 operators updated
-- ============================================
