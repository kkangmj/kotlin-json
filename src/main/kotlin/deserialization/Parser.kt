package ru.yole.jkid.deserialization

import java.io.Reader

class Parser(reader: Reader, val rootObject: JsonObject) {
    // Lexer는 reader 파싱하는데 도움 주는 클래스
    private val lexer = Lexer(reader)

    fun parse() {
        expect(Token.LBRACE)    // { 로 시작하는지 체크
        // rootObject는 Seed 타입
        parseObjectBody(rootObject)
        if (lexer.nextToken() != null) {
            throw IllegalArgumentException("Too many tokens")
        }
    }

    private fun parseObjectBody(jsonObject: JsonObject) {
        parseCommaSeparated(Token.RBRACE) { token ->
            if (token !is Token.StringValue) {
                throw MalformedJSONException("Unexpected token $token")
            }

            val propName = token.value    // title
            expect(Token.COLON)
            parsePropertyValue(jsonObject, propName, nextToken())    // Catch-22
        }
    }

    private fun parseArrayBody(currentObject: JsonObject, propName: String) {
        parseCommaSeparated(Token.RBRACKET) { token ->
            parsePropertyValue(currentObject, propName, token)
        }
    }

    private fun parseCommaSeparated(stopToken: Token, body: (Token) -> Unit) {
        var expectComma = false
        while (true) {
            var token = nextToken()
            // stopToken = } (Token.RBRACE)
            if (token == stopToken) return
            if (expectComma) {
                if (token != Token.COMMA) throw MalformedJSONException("Expected comma")
                token = nextToken()
            }

            body(token)

            expectComma = true
        }
    }

    private fun parsePropertyValue(currentObject: JsonObject, propName: String, token: Token) {
        when (token) {
            is Token.ValueToken ->
                currentObject.setSimpleProperty(propName, token.value)

            Token.LBRACE -> {   // {  = Token.LBRACE
                val childObj = currentObject.createObject(propName)    // createCompositeProperty 함수
                parseObjectBody(childObj)
            }

            Token.LBRACKET -> {    // [ = Token.LBRACKET
                val childObj = currentObject.createArray(propName)    // createCompositeProperty 함수
                parseArrayBody(childObj, propName)
            }

            else ->
                throw MalformedJSONException("Unexpected token $token")
        }
    }

    private fun expect(token: Token) {
        if (lexer.nextToken() != token) {
            throw IllegalArgumentException("$token expected")
        }
    }

    private fun nextToken(): Token = lexer.nextToken() ?: throw IllegalArgumentException("Premature end of data")
}
