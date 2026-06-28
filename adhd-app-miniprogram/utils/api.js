const app = getApp();

function doRequest({ url, method = 'GET', data }) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `${app.globalData.baseUrl}${url}`,
      method,
      data,
      header: {
        'content-type': 'application/json',
        Authorization: `Bearer ${app.globalData.token}`,
      },
      success: (res) => {
        if (res.statusCode >= 200 && res.statusCode < 300 && res.data.code === 0) {
          resolve(res.data.data);
          return;
        }
        reject({
          statusCode: res.statusCode,
          ...(res.data || {}),
        });
      },
      fail: reject,
    });
  });
}

function decodeChunkData(data) {
  if (typeof data === 'string') {
    return data;
  }
  if (typeof TextDecoder !== 'undefined') {
    return new TextDecoder('utf-8').decode(data);
  }

  const bytes = new Uint8Array(data);
  let text = '';
  for (let i = 0; i < bytes.length; i += 1) {
    text += String.fromCharCode(bytes[i]);
  }
  return decodeURIComponent(escape(text));
}

function request({ url, method = 'GET', data }, allowRetry = true) {
  return app.ensureLogin()
    .then(() => doRequest({ url, method, data }))
    .catch((error) => {
      if (allowRetry && error && error.statusCode === 401) {
        clearSession();
        return app.ensureLogin().then(() => request({ url, method, data }, false));
      }
      throw error;
    });
}

function getProfile() {
  return request({ url: '/users/me' });
}

function checkIn(taskId, note = '') {
  return request({
    url: '/checkins',
    method: 'POST',
    data: { taskId, note },
  });
}

function login() {
  return app.ensureLogin();
}

function clearSession() {
  app.globalData.token = '';
  app.globalData.user = null;
  wx.removeStorageSync('token');
  wx.removeStorageSync('userProfile');
}

function getDashboard() {
  return request({ url: '/users/me/dashboard' });
}

function getTasks() {
  return request({ url: '/tasks' });
}

function createTask(data) {
  return request({ url: '/tasks', method: 'POST', data });
}

function getWeeklyReport() {
  return request({ url: '/reports/weekly' });
}

function sendChatMessage(message) {
  return request({
    url: '/ai/chat',
    method: 'POST',
    data: { message },
  });
}

function getChatHistory() {
  return request({ url: '/ai/chat/history' });
}

function doStreamChatMessage(message, handlers = {}) {
  return new Promise((resolve, reject) => {
    let buffer = '';
    let completed = false;
    let requestTask;

    const finish = () => {
      if (completed) {
        return;
      }
      completed = true;
      resolve();
    };

    const fail = (error) => {
      if (completed) {
        return;
      }
      completed = true;
      reject(error);
    };

    const consume = (chunk) => {
      buffer += chunk.replace(/\r\n/g, '\n');

      if (!buffer.includes('\n\n')) {
        return;
      }

      const segments = buffer.split('\n\n');
      buffer = segments.pop() || '';

      segments.forEach((segment) => {
        const lines = segment.split('\n');
        let eventName = 'message';
        const dataLines = [];

        lines.forEach((line) => {
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim();
            return;
          }
          if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).trim());
          }
        });

        if (!dataLines.length) {
          return;
        }

        const rawData = dataLines.join('\n');
        if (eventName === 'open') {
          return;
        }
        if (eventName === 'done' || rawData === '[DONE]') {
          finish();
          return;
        }

        let data = rawData;
        if (rawData.startsWith('{')) {
          try {
            const parsed = JSON.parse(rawData);
            data = parsed.delta || '';
          } catch (error) {
            data = rawData;
          }
        }

        if (handlers.onChunk) {
          handlers.onChunk(data);
        }
      });
    };

    requestTask = wx.request({
      url: `${app.globalData.baseUrl}/ai/chat/stream`,
      method: 'POST',
      data: { message },
      enableChunked: true,
      responseType: 'text',
      header: {
        'content-type': 'application/json',
        Authorization: `Bearer ${app.globalData.token}`,
      },
      success: (res) => {
        if (res.statusCode < 200 || res.statusCode >= 300) {
          fail({ statusCode: res.statusCode, ...(res.data || {}) });
          return;
        }

        if (typeof res.data === 'string' && res.data) {
          if (res.data.includes('data:')) {
            consume(res.data);
          } else if (handlers.onChunk) {
            handlers.onChunk(res.data);
          }
        }

        if (buffer.trim() === '[DONE]') {
          finish();
          return;
        }

        if (!completed) {
          finish();
        }
      },
      fail,
    });

    if (requestTask && requestTask.onChunkReceived) {
      requestTask.onChunkReceived((chunkRes) => {
        consume(decodeChunkData(chunkRes.data));
      });
    }
  });
}

function streamChatMessage(message, handlers = {}, allowRetry = true) {
  return app.ensureLogin()
    .then(() => doStreamChatMessage(message, handlers))
    .catch((error) => {
      if (allowRetry && error && error.statusCode === 401) {
        clearSession();
        return app.ensureLogin().then(() => streamChatMessage(message, handlers, false));
      }
      throw error;
    });
}

module.exports = {
  login,
  clearSession,
  getProfile,
  getDashboard,
  getTasks,
  createTask,
  checkIn,
  getWeeklyReport,
  getChatHistory,
  sendChatMessage,
  streamChatMessage,
};
