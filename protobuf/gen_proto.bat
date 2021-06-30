
echo "starting proto batch in %cd%"
set SOURCE="../src/main/protobuf"
set OUT="../src/main/java"
call protoc.exe --proto_path=%SOURCE% --java_out=%OUT% %SOURCE%/*.proto
