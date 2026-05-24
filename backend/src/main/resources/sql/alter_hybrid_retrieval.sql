ALTER TABLE `kb_chunk`
ADD FULLTEXT INDEX `ft_chunk_text` (`chunk_text`) WITH PARSER ngram;
