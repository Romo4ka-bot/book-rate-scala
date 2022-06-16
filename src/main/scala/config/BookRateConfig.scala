package config

final case class ServerConfig(host: String, port: Int)
final case class BookRateConfig(db: DatabaseConfig, server: ServerConfig)
