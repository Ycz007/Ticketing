import test from 'node:test'
import assert from 'node:assert/strict'
import { categories, resultOptions, statuses, time } from '../src/domain/ticket.ts'

test('五种反馈分类与五种工单状态独立', () => {
  assert.equal(Object.keys(categories).length, 5)
  assert.equal(Object.keys(statuses).length, 5)
  assert.equal(categories.UNCONFIRMED, '待确认')
  assert.equal(statuses.AWAITING_CONFIRMATION, '待确认完成')
})
test('功能建议允许拒绝或暂缓，咨询仅提供解答结论', () => {
  assert.deepEqual(resultOptions('FEATURE_SUGGESTION'), ['ACCEPTED', 'DEFERRED', 'REJECTED'])
  assert.deepEqual(resultOptions('USAGE_QUESTION'), ['ANSWERED'])
})
test('权限申请通过不等于系统故障修复', () => {
  assert.deepEqual(resultOptions('ACCESS_REQUEST'), ['APPROVED', 'REJECTED'])
  assert.deepEqual(resultOptions('SYSTEM_FAILURE'), ['RESOLVED'])
})
test('未知类别不提供可提交结果', () => { assert.deepEqual(resultOptions('UNKNOWN'), []) })
test('服务端UTC时间按北京时间显示', () => { assert.match(time('2026-10-21T00:00:00'), /8:00:00|08:00:00/) })
