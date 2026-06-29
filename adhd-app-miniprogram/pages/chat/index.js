const api = require('../../utils/api');
const { markdownToHtml } = require('../../utils/markdown');

function formatMessage(role, content, streaming = false) {
  return {
    role,
    content,
    html: markdownToHtml(content),
    streaming,
  };
}

function applyAssistantMessage(page, assistantIndex, content, streaming = false) {
  const updatedMessages = page.data.messages.slice();
  if (!updatedMessages[assistantIndex]) {
    return;
  }
  updatedMessages[assistantIndex] = formatMessage('assistant', content, streaming);
  page.setData({ messages: updatedMessages }, () => page.scrollToBottom());
}

const DEFAULT_MESSAGES = [
  formatMessage('assistant', '先别急着责备自己。你已经在很努力地陪孩子了。告诉我，今天最崩溃的是哪一刻？'),
];

Page({
  bottomScrollTimer: null,

  data: {
    inputValue: '',
    messages: DEFAULT_MESSAGES,
    isSending: false,
    isLoadingHistory: false,
    scrollIntoView: 'chat-bottom',
    scrollTop: 0,
  },

  onInput(event) {
    this.setData({ inputValue: event.detail.value });
  },

  onShow() {
    api.login()
      .then(() => this.loadHistory())
      .catch((error) => {
        wx.showToast({ title: (error && error.message) || '微信登录失败', icon: 'none' });
      });
  },

  scrollToBottom() {
    if (this.bottomScrollTimer) {
      clearTimeout(this.bottomScrollTimer);
      this.bottomScrollTimer = null;
    }

    this.setData({ scrollIntoView: '', scrollTop: 999999 }, () => {
      wx.nextTick(() => {
        this.setData({ scrollIntoView: 'chat-bottom', scrollTop: 999999 });
      });
    });

    this.bottomScrollTimer = setTimeout(() => {
      this.setData({ scrollIntoView: '', scrollTop: 999999 }, () => {
        wx.nextTick(() => {
          this.setData({ scrollIntoView: 'chat-bottom', scrollTop: 999999 });
        });
      });
      this.bottomScrollTimer = null;
    }, 260);
  },

  onUnload() {
    if (this.bottomScrollTimer) {
      clearTimeout(this.bottomScrollTimer);
      this.bottomScrollTimer = null;
    }
  },

  loadHistory() {
    this.setData({ isLoadingHistory: true });

    api.getChatHistory()
      .then((history) => {
        if (Array.isArray(history) && history.length) {
          this.setData({
            messages: history.map((item) => formatMessage(item.role, item.content)),
            isLoadingHistory: false,
          }, () => this.scrollToBottom());
          return;
        }

        this.setData({ messages: DEFAULT_MESSAGES, isLoadingHistory: false }, () => this.scrollToBottom());
      })
      .catch(() => {
        this.setData({ messages: DEFAULT_MESSAGES, isLoadingHistory: false }, () => this.scrollToBottom());
      });
  },

  sendMessage() {
    const { inputValue, messages } = this.data;
    const trimmed = inputValue.trim();
    if (!trimmed || this.data.isSending) {
      return;
    }

    const nextMessages = messages.concat(
      formatMessage('user', trimmed),
      formatMessage('assistant', '', true)
    );

    this.setData({
      messages: nextMessages,
      inputValue: '',
      isSending: true,
    }, () => this.scrollToBottom());

    const assistantIndex = nextMessages.length - 1;

    api.streamChatMessage(trimmed, {
      onChunk: (chunk) => {
        const assistantMessage = this.data.messages[assistantIndex];
        if (!assistantMessage) {
          return;
        }

        const content = assistantMessage.content + chunk;
        applyAssistantMessage(this, assistantIndex, content, true);
      },
    })
      .then(() => {
        const assistantMessage = this.data.messages[assistantIndex];
        if (!assistantMessage) {
          this.setData({ isSending: false });
          return;
        }

        if (assistantMessage.content) {
          applyAssistantMessage(this, assistantIndex, assistantMessage.content, false);
          this.setData({ isSending: false });
          return;
        }

        api.sendChatMessage(trimmed)
          .then((response) => {
            applyAssistantMessage(this, assistantIndex, response.reply || 'AI 暂时没有回应，请稍后再试。', false);
            this.setData({ isSending: false });
          })
          .catch((error) => {
            applyAssistantMessage(this, assistantIndex, 'AI 暂时没有回应，请稍后再试。', false);
            this.setData({ isSending: false });
            wx.showToast({ title: (error && error.msg) || (error && error.message) || 'AI 暂时没有回应', icon: 'none' });
          });
      })
      .catch((error) => {
        const assistantMessage = this.data.messages[assistantIndex];
        if (!assistantMessage || !assistantMessage.content) {
          applyAssistantMessage(this, assistantIndex, 'AI 暂时没有回应，请稍后再试。', false);
        } else {
          applyAssistantMessage(this, assistantIndex, assistantMessage.content, false);
        }
        this.setData({ isSending: false });
        wx.showToast({ title: (error && error.msg) || (error && error.message) || 'AI 暂时没有回应', icon: 'none' });
      });
  },
});
