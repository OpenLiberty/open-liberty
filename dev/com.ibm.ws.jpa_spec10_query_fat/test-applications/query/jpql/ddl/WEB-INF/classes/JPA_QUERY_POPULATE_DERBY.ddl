
INSERT INTO ${schemaname}.SimpleJPQL 
    (id, 
     boolean1, boolean2, boolean3, byte1, byte2, byte3, char1, char2, char3,
     double1, double2, double3, float1, float2, float3, int1, int2, int3,
     long1, long2, long3, short1, short2, short3, 
     str1,
     str2,
     str3) 
VALUES 
    (1,
     1, 0, 0, 1, 2, 3, 'a', 'b' , 'c',
     1.1, 2.2, 3.3, 100.1, 200.2, 300.3, 42, 142, 242,
     1000, 2000, 3000, 128, 256, 512,
     'String 1',
     'String 2',
     'String 3'
    );