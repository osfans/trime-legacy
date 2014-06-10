trime
=====
源於開源的[注音倉頡輸入法]，
參考著名的[Rime]輸入法，
藉助數據庫與正則表達式，
使用安卓的java語言實現的
一款針對方言的漢字輸入法。

## 起源
最初輸入法是寫給[泰如拼音]（thae5 rv2）的，首字母爲t、r。
當然，你完全可以認爲是TiaRectInputMEthod的縮寫。

## 針對方言
- 反查字音
- 輸出拼音
- 模糊音
- 國際音標同音字表
- 支持unicode ext-A/B/C/D大字庫方言正字

## 使用自定義方案
- 使用[trime-tool]生成自己的trime.db，放到assests目錄下，重新編譯生成新apk。
- 從設置中導入trime.db

## 已有功能
- 自定義方案：拼音、注音、音標。長按漢字鍵，切換方案。短按漢字鍵，切換中英文。
- 自定義鍵盤：全拼、雙拼、字母、音標、漢字。短按拼音鍵，切換鍵盤。
- 模糊音：尖團、平翹、前後鼻音。長按拼音鍵，查看方案信息，設置模糊音。
- 詞語輸入：支持簡拼，自動音節切分。短按逗號或'，手動切分音節。
- 詞語聯想：自動根據最後一個字進行聯想。
- 輸出簡體：啓用時，候選字爲繁體，輸出時使用[opencc]轉換爲簡體。
- 輸出拼音：啓用時，自動在輸出的漢字後加括號和注音。
- 反查字音：長按逗號，查詢光標處漢字讀音。

## 將有功能
- 詞頻調整
- 自動造詞
- 顯示漢字備註信息：候選或反查時顯示，當字典用

[trime-tool]: https://github.com/osfans/trime
[opencc]: https://github.com/BYVoid/OpenCC
[Rime]: https://code.google.com/p/rimeime/
[注音倉頡輸入法]: https://code.google.com/p/android-traditional-chinese-ime/
[泰如拼音]: http://tieba.baidu.com/f?kw=%E6%B3%B0%E5%A6%82
