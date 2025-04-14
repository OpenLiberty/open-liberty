# Instruction to generate rsa_key_nohost.jks and rsa_trust_nohost.jks
keytool -genkeypair -alias rsacert -keystore rsa_key_nohost.jks -storetype jks -storepass Liberty -keypass Liberty -keyalg RSA -keysize 2048 -validity 20803 
-dname "CN=new_user2, O=IBM, C=US" -ext san=dns:nohost
keytool -export -alias rsacert -keystore rsa_key_nohost.jks -storepass Liberty -storetype jks -file rsacert.cer
keytool -import -alias rsacert -keystore rsa_trust_nohost.jks -storetype jks -storepass LibertyServer -trustcacerts -noprompt -file rsacert.cer

keytool -genkeypair -alias rsacert_new -keystore rsacert_new.jks -storetype jks -storepass Liberty -keypass Liberty -keyalg RSA -keysize 2048 -validity 20803 
-dname "CN=new_user2, O=IBM, C=US" -ext san=dns:localhost
keytool -export -alias rsacert_new -keystore rsacert_new.jks -storepass Liberty -storetype jks -file rsacertnew.cer
keytool -import -alias rsacert_new -keystore rsa_trust_nohost.jks -storetype jks -storepass LibertyServer -trustcacerts -noprompt -file rsacertnew.cer