Embulk::JavaPlugin.register_encoder(
  "encrypted_zip", "org.embulk.encoder.encrypted_zip.EncryptedZipEncoderPlugin",
  File.expand_path('../../../../classpath', __FILE__))
