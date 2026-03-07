CREATE TABLE chat_usage (                                                                                                                  
      id          VARCHAR(60) PRIMARY KEY,                                                                                                   
      user_id     VARCHAR(60) NOT NULL REFERENCES users(id),                                                                                 
      used_at     TIMESTAMP WITH TIME ZONE NOT NULL,                                                                                         
      created_at  TIMESTAMP WITH TIME ZONE NOT NULL
  );

  CREATE INDEX idx_chat_usage_user_used_at ON chat_usage(user_id, used_at);
