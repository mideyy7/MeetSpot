package com.meetspot.app.data

class FakeLocationHelper : LocationHelper {
    var result: Result<String> = Result.success("51.500000,-0.130000")
    var callCount = 0

    override suspend fun currentLocationString(): Result<String> {
        callCount++
        return result
    }
}
