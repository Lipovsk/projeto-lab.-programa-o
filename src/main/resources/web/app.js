'use strict';
const input = document.querySelector('#file');
const choose = document.querySelector('#choose');
const analyze = document.querySelector('#analyze');
const status = document.querySelector('#status');
const results = document.querySelector('.results');
const keys = ['homo', 'lumo', 'gapHartree', 'gapEv'];
let selectedFile = null;
function setStatus(message, state = '') {
  status.querySelector('span').textContent = message;
  status.dataset.state = state;
}
function clearResults() {
  keys.forEach(key => { document.getElementById(key).textContent = '—'; });
}
function showError(message) {
  setStatus('Erro ao analisar arquivo.', 'error');
  document.querySelector('#error-message').textContent = message;
  document.querySelector('#error-dialog').showModal();
}
choose.addEventListener('click', () => input.click());
input.addEventListener('change', () => {
  const file = input.files[0];
  if (!file) return;
  clearResults();
  selectedFile = null;
  analyze.disabled = true;
  document.querySelector('#filename').textContent = 'Nenhum arquivo selecionado';
  if (!/\.(out|log|txt)$/i.test(file.name)) {
    input.value = '';
    showError('Selecione um arquivo .out, .log ou .txt.');
    return;
  }
  selectedFile = file;
  document.querySelector('#file-label').textContent = 'Arquivo selecionado:';
  document.querySelector('#filename').textContent = file.name;
  analyze.disabled = false;
  setStatus('Arquivo selecionado. Clique em ANALISAR para iniciar.');
});
analyze.addEventListener('click', async () => {
  if (!selectedFile) return;
  choose.disabled = analyze.disabled = true;
  results.setAttribute('aria-busy', 'true');
  clearResults();
  setStatus('Analisando arquivo...', 'busy');
  try {
    const response = await fetch('/api/analisar', {
      method: 'POST', headers: {'Content-Type': 'application/octet-stream'}, body: selectedFile
    });
    if (!response.ok) throw new Error(await response.text());
    const data = await response.json();
    if (!keys.every(key => typeof data[key] === 'number' && Number.isFinite(data[key]))) {
      throw new Error('A análise retornou valores inválidos.');
    }
    keys.forEach(key => { document.getElementById(key).textContent = data[key].toFixed(6); });
    setStatus('Análise concluída com sucesso.', 'success');
  } catch (error) {
    showError(error instanceof TypeError
      ? 'Não foi possível ler o arquivo ou conectar ao analisador. Verifique se o programa Java está em execução.'
      : error.message);
  } finally {
    choose.disabled = analyze.disabled = false;
    results.setAttribute('aria-busy', 'false');
  }
});
